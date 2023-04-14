package forex.services.rates.interpreters

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.ApplicationConfig
import forex.domain.{ CurrenciesPair, Currency, Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._
import io.circe
import org.http4s.client._

import scala.concurrent.ExecutionContext.global
import cats.effect.Blocker

import java.util.concurrent._
import cats.effect._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.headers._
import org.http4s.MediaType
import org.http4s.Method._
import org.http4s.util.CaseInsensitiveString
import io.circe.parser.decode
import com.redis.RedisClient
import forex.domain.CurrenciesPair.PairCode
import forex.domain.Currency.USD
import forex.domain.logic.TransitiveExchangeRate
import forex.domain.logic.TransitiveExchangeRate.{
  pairAsTransitiveExchangeRates,
  OppositeToRefRateWrapper,
  ReferenceRateWrapper
}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.jdk.DurationConverters
import org.slf4j.LoggerFactory

// TODO PR (high) - Adjust reduce of calculated BigDecimal for rates generated by approximation of ones given by OneFrame
class OneFrameLive[F[_]: Applicative](config: ApplicationConfig) extends Algebra[F] {
  import OneFrameLive._

  implicit private val cs: ContextShift[IO] = IO.contextShift(global)

  // TODO PR (high) - consider moving dependencies outside the service to avoid multiple instantiations
  private val blockingPool           = Executors.newFixedThreadPool(5)
  private val blocker                = Blocker.liftExecutorService(blockingPool)
  private val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

  private val oneFrameConfig = config.webServices.oneFrame
  private val redisConfig    = config.webServices.redis

  // TODO PR (high) - Wrap into a service to mock and test
  // TODO PR (low) - Wrap into a service exposing only useful encapsulated methods
  // TODO PR (low) - refactor and rename to be stack agnostic
  private val redis = new RedisClient(host = redisConfig.host, port = redisConfig.port)

  private def getRateFromRedis(key: PairCode): Option[String] =
    redis.get[String](key)
  private def getRateFromRedis(key: CurrenciesPair): Option[String] =
    getRateFromRedis(key = key.pairCode)

  private lazy val referencePairCodes =
    Currency.all
      .filterNot(_ == USD)
      .map(currency => CurrenciesPair(from = USD, to = currency).pairCode)

  private lazy val requestAllReferenceRateWrappersUri = {
    val pairParams =
      referencePairCodes.map(pairCode => s"pair=$pairCode")
    // TODO PR (low) - consider to use https
    Uri.unsafeFromString(
      s"http://${oneFrameConfig.host}:${oneFrameConfig.port}/rates?${pairParams.mkString("&")}"
    )
  }

  private lazy val requestAllReferenceRateWrappers = {
    logger.trace(s"""requestAllReferenceRateWrappers :
         |requestAllReferenceRateWrappersUri=$requestAllReferenceRateWrappersUri ;
         |token=10dc303535874aeccc86a8251e6992f5
         |""".stripMargin.replace('\n', ' '))
    GET(
      requestAllReferenceRateWrappersUri,
      Header.Raw(
        name = CaseInsensitiveString("token"),
        value = "10dc303535874aeccc86a8251e6992f5"
      ),
      Accept(MediaType.application.json)
    )
  }

  override def getExchangeRate(pair: CurrenciesPair): F[RatesServiceError Either Rate] =
    if (pair.isOnSameCurrency) {
      getRateForSameCurrencyPair(currency = pair.from)
    } else {
      getRateForDifferentCurrenciesPair(pair)
    }

  private def getRateForSameCurrencyPair(currency: Currency): F[RatesServiceError Either Rate] = {
    val rate = Rate(
      pair = CurrenciesPair.ofSameCurrency(currency),
      price = Price(BigDecimal(1)),
      timestamp = Timestamp.now
    )
    logger.debug(s"""getRateForSameCurrencyPair(currency=$currency) :
         |Generate neutral rate on same currency = $rate
         |""".stripMargin.replace('\n', ' '))
    rate.asRight[RatesServiceError].pure[F]
  }

  // TODO PR (high) - Implement fetching from cache + refreshing on too old
  // TODO PR (low) - Implement refreshing in a different fiber
  private def getRateForDifferentCurrenciesPair(pair: CurrenciesPair): F[RatesServiceError Either Rate] = {
    val transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]] =
      pairAsTransitiveExchangeRates(pair)

    logger.debug(s"""getRateForDifferentCurrenciesPair(pair=$pair) :
         |Transitive Exchange Rate pairs = $transitivePairs
         |""".stripMargin.replace('\n', ' '))

    def isRecentEnoughRate(rate: Rate): Boolean =
      rate.moreRecentThan(DurationConverters.ScalaDurationOps(oneFrameConfig.freshness).toJava)

    val tooOldRatesError =
      RatesServiceError.OneFrameLookupFailed(s"Recent rates not found for ${pair.pairCode}")
    val errorOrRate: Either[RatesServiceError, Rate] =
      getRateFromCache(transitivePairs).filterOrElse[RatesServiceError](isRecentEnoughRate, zero = tooOldRatesError)

    errorOrRate.left
      .flatMap { _ =>
        logger.debug(s"""getRateForDifferentCurrenciesPair(pair=$pair) :
             |Cache data too old
             |""".stripMargin.replace('\n', ' '))
        refreshCache()
        getRateFromCache(transitivePairs).left
          .map(
            _ =>
              RatesServiceError.OneFrameLookupFailed(s"Recent rates not found for ${pair.pairCode}"): RatesServiceError
          )
      }
      .pure[F]
  }

  // TODO PR (high) - improve this naive implementation - consider using a pool
  // TODO PR (low) - use returned type to handle error in a more elegant way
  private def refreshCache(): Unit = {
    logger.debug("refreshCache() : Start")
    val preparedRequest = httpClient.expect[String](requestAllReferenceRateWrappers)

    // TODO PR (low) - find a more elegant way to lift IO as an Applicative F
    val results = preparedRequest.unsafeRunSync()

    import OneFrameResponse.rateDecoderFromResponse
    val decodedResponse: Either[circe.Error, List[Rate]] = decode[List[Rate]](results)

    val errorOrRates = oneFrameResponseToRatesOrServiceError(decodedResponse)
    errorOrRates.foreach { rates =>
      val areUpdatesSucceeded: Seq[(PairCode, Boolean)] =
        rates.map { rate =>
          logger.debug(s"""refreshCache() :
               |Fresh rate from OneFrame service = $rate
               |""".stripMargin.replace('\n', ' '))
          val succeeded =
            redis.set(rate.pairCode, rate.asJson)
          (rate.pairCode, succeeded)
        }

      val notUpdated: Seq[PairCode] = areUpdatesSucceeded
        .filterNot { case (_, succeeded) => succeeded }
        .map { case (pairCode, _) => pairCode }
      val hasNotUpdatedRefs = notUpdated.nonEmpty
      if (hasNotUpdatedRefs && logger.isWarnEnabled) {
        notUpdated.foreach { pairCode =>
          logger.warn(s"""refreshCache() :
               |No valid value returned by OneFrame for Reference Pair = $pairCode
               |""".stripMargin.replace('\n', ' '))
        }
      }
    }
    logger.debug("refreshCache() : End")
  }

  private def getRateFromCache(
      transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]]
  ): Either[RatesServiceError, Rate] = {
    val pairCodes =
      transitivePairs.map(_.referenceValue.pairCode)
    val errorOrCacheResults =
      getTransitiveRatesAsStringsFromCache(transitivePairs)
        .toRight(RatesServiceError.OneFrameLookupFailed(s"Rates not found for ${pairCodes.mkString(" and ")}"))

    decodeAndMergeRates(errorOrCacheResults)
  }

  private def getTransitiveRatesAsStringsFromCache(
      transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]]
  ): Option[List[TransitiveExchangeRate[String]]] =
    transitivePairs match {
      case List(oppositeToRefPair: OppositeToRefRateWrapper[CurrenciesPair]) =>
        getOppositeRefRatesAsStringsFromCache(oppositeToRefPair)
          .map(List.apply(_))

      case List(referenceRate: ReferenceRateWrapper[CurrenciesPair]) =>
        getReferenceRatesAsStringsFromCache(referenceRate)
          .map(List.apply(_))

      case List(
          oppositeToRefPair: OppositeToRefRateWrapper[CurrenciesPair],
          referenceRate: ReferenceRateWrapper[CurrenciesPair]
          ) =>
        for {
          oppositeRefRate <- getOppositeRefRatesAsStringsFromCache(oppositeToRefPair)
          referenceRate <- getReferenceRatesAsStringsFromCache(referenceRate)
        } yield List(oppositeRefRate, referenceRate)

      case _ =>
        None
    }

  // TODO PR (low) Test - important, but requires stub or mock for redis
  private def getReferenceRatesAsStringsFromCache(referencePairWrapper: ReferenceRateWrapper[CurrenciesPair]) =
    getRateFromRedis(key = referencePairWrapper.referenceValue)
      .map(rate => ReferenceRateWrapper(rate))

  // TODO PR (low) Test - important, but requires stub or mock for redis
  private def getOppositeRefRatesAsStringsFromCache(
      oppositeToRefPairWrapper: OppositeToRefRateWrapper[CurrenciesPair]
  ) =
    getRateFromRedis(key = oppositeToRefPairWrapper.referenceValue)
      .map(rate => OppositeToRefRateWrapper(rate))

}

object OneFrameLive {

  // TODO PR (low) - Handle logger dependency in a better way
  private def logger = LoggerFactory.getLogger(this.getClass)

  // TODO PR (high) - Test this well
  private[interpreters] def decodeAndMergeRates(
                                                 errorOrCacheResults: Either[RatesServiceError, List[TransitiveExchangeRate[String]]]
  ): Either[RatesServiceError, Rate] =
    for {
      wrappedEncodedRates <- errorOrCacheResults
      errorsOrRates = decodeWrappedTransitiveEncodedRates(wrappedEncodedRates)
      mergedRate <- mergeErrorsOrRates(errorsOrRates)
      _ <- Right(
            logger.debug(s"""decodeAndMergeRates() :
             |mergedRate = $mergedRate
             |""".stripMargin.replace('\n', ' '))
          )
    } yield mergedRate

  private[interpreters] def decodeWrappedTransitiveEncodedRates(
      wrappedEncodedRated: List[TransitiveExchangeRate[String]]
  ): List[Either[RatesServiceError, TransitiveExchangeRate[Rate]]] =
    wrappedEncodedRated.map {
      case ReferenceRateWrapper(rateAsString) =>
        rateDecoderFromCacheOrServiceError(rateAsString)
          .map(ReferenceRateWrapper.apply): Either[RatesServiceError, TransitiveExchangeRate[Rate]]

      case OppositeToRefRateWrapper(rateAsString) =>
        rateDecoderFromCacheOrServiceError(rateAsString)
          .map(OppositeToRefRateWrapper.apply): Either[RatesServiceError, TransitiveExchangeRate[Rate]]
    }

  private[interpreters] def mergeErrorsOrRates(
      errorsOrRates: List[Either[RatesServiceError, TransitiveExchangeRate[Rate]]]
  ) =
    errorsOrRates match {
      case Left(error) :: _ =>
        Left(error)

      case _ :: Left(error) :: _ =>
        Left(error)

      case List(Right(OppositeToRefRateWrapper(referenceRateForOpposite))) =>
        Right(referenceRateForOpposite.opposite)

      case List(Right(ReferenceRateWrapper(referenceRate))) =>
        Right(referenceRate)

      case List(
          Right(oppositeWrapper: OppositeToRefRateWrapper[Rate]),
          Right(referenceWrapper: ReferenceRateWrapper[Rate])
          ) =>
        val mergedRate =
          TransitiveExchangeRate.rateByTransitivity(oppositeWrapper, referenceWrapper)
        Right(mergedRate)

      case _ =>
        Left(RatesServiceError.OneFrameLookupFailed(s"Currently impossible to calculate this rate."))
    }

  private[interpreters] def rateDecoderFromCacheOrServiceError(rateAsString: String) = {
    val errorOrDecoded = decode[Rate](rateAsString)

    def convertError(error: circe.Error): RatesServiceError =
      RatesServiceError.OneFrameDecodeFailed(s"Impossible to extract rate data: ${error.getMessage}")

    errorOrDecoded.left.map(convertError)
  }

  private def oneFrameResponseToRatesOrServiceError(
      decodedResponse: Either[circe.Error, List[Rate]],
  ): Either[RatesServiceError, List[Rate]] =
    decodedResponse.left.map { error =>
      val message = s"""No valid rate found for pair: "${error.getMessage}"""""
      RatesServiceError.OneFrameDecodeFailed(message)
    }

}