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
import forex.services.rates.interpreters.TransitiveExchangeRate.{
  pairAsTransitiveExchangeRates,
  OppositeToRefRateWrapper,
  ReferenceRateWrapper
}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.jdk.DurationConverters

class OneFrameLive[F[_]: Applicative](config: ApplicationConfig) extends Algebra[F] {
  implicit private val cs: ContextShift[IO] = IO.contextShift(global)

  // TODO PR (high) - Add Debugs logs on key moment
  // "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
//    private val logger = com.typesafe.scalalogging.Logger(getClass)
//  logger.debug("Hello, logging!")

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
    println(s""" ### requestAllReferenceRateWrappers ###
         |requestAllReferenceRateWrappersUri: $requestAllReferenceRateWrappersUri
         |token: 10dc303535874aeccc86a8251e6992f5
         |""".stripMargin) // TODO PR DEBUG
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

  // TODO PR (high) - Implement fetching from cache + refreshing on too old
  // TODO PR (low) - Implement refreshing in a different fiber
  private def getRateForDifferentCurrenciesPair(pair: CurrenciesPair): F[RatesServiceError Either Rate] = {

    val transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]] =
      pairAsTransitiveExchangeRates(pair)(CurrenciesPair.apply)

    def isRecentEnoughRate(rate: Rate): Boolean =
      rate.moreRecentThan(DurationConverters.ScalaDurationOps(oneFrameConfig.freshness).toJava)

    val tooOldRatesError =
      RatesServiceError.OneFrameLookupFailed(s"Recent rates not found for ${pair.pairCode}")
    val errorOrRate: Either[RatesServiceError, Rate] =
      getRateFromCache(transitivePairs).filterOrElse[RatesServiceError](isRecentEnoughRate, zero = tooOldRatesError)

    errorOrRate.left
      .flatMap { _ =>
        println(s"""
                 |
                 |RECENT ENOUGH RATE NOT FOUND !
                 |
                 |""".stripMargin) // TODO PR DEBUG - TODO improve debug message
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
    val preparedRequest = httpClient.expect[String](requestAllReferenceRateWrappers)

    println(s"""
         |
         |########################################
         |RESHRESH CACHE !
         |########################################
         |
         |""".stripMargin) // TODO PR INFO - improve

    // TODO PR (low) - find a more elegant way to lift IO as an Applicative F
    val results = preparedRequest.unsafeRunSync()

    import OneFrameResponse.decodeRateFromResponse
    val decodedResponse: Either[circe.Error, List[Rate]] = decode[List[Rate]](results)

    // TODO PR (low) - uniformize either naming
    val errorOrRates = oneFrameResponseToRatesOrServiceError(decodedResponse)
    errorOrRates.foreach { rates =>
      val areUpdatesSucceeded: Seq[(PairCode, Boolean)] =
        rates.map { rate =>
          println(s"rate: $rate") // TODO PR DEBUG
          val succeeded =
            redis.set(rate.pairCode, rate.asJson)
          (rate.pairCode, succeeded)
        }

      println(s"Cache updated count: ${areUpdatesSucceeded.size} / ${referencePairCodes.size}") // TODO PR DEBUG
      val notUpdated: Seq[PairCode] = areUpdatesSucceeded
        .filterNot { case (_, succeeded) => succeeded }
        .map { case (pairCode, _) => pairCode }
      val hasNotUpdatedRefs = notUpdated.nonEmpty
      if (hasNotUpdatedRefs) { // TODO PR WARNING
        notUpdated.foreach { pairCode =>
          println(s"Not valid value returned by OneFrame for Reference Pair: $pairCode") // TODO PR WARNING
        }
      }
    }
  }

  private def getRateFromCache(
      transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]]
  ): Either[RatesServiceError, Rate] = {
    val pairCodes =
      transitivePairs.map(_.referenceValue.pairCode)
    val cacheResultsOrError =
      getTransitiveRatesAsStringsFromCache(transitivePairs)
        .toRight(RatesServiceError.OneFrameLookupFailed(s"Rates not found for ${pairCodes.mkString(" and ")}"))

    encodeAndMergeRates(cacheResultsOrError)
  }

  // TODO PR (high) - Test this well
  private def encodeAndMergeRates(
      cacheResultsOrError: Either[RatesServiceError, List[TransitiveExchangeRate[String]]]
  ): Either[RatesServiceError, Rate] =
    for {
      results <- cacheResultsOrError
      errorsOrRates: List[Either[RatesServiceError, TransitiveExchangeRate[Rate]]] = results.map {
        case ReferenceRateWrapper(rateAsString) =>
          decodeRateFromCacheOrServiceError(rateAsString)
            .map(ReferenceRateWrapper.apply): Either[RatesServiceError, TransitiveExchangeRate[Rate]]

        case OppositeToRefRateWrapper(rateAsString) =>
          decodeRateFromCacheOrServiceError(rateAsString)
            .map(OppositeToRefRateWrapper.apply): Either[RatesServiceError, TransitiveExchangeRate[Rate]]
      }
      mergedRate <- errorsOrRates match {
                     case List(Left(error), _) =>
                       Left(error)

                     case List(_, Left(error)) =>
                       Left(error)

                     case List(Right(OppositeToRefRateWrapper(referenceRateForOpposite))) =>
                       Right(referenceRateForOpposite.opposite)

                     case List(Right(ReferenceRateWrapper(referenceRate))) =>
                       Right(referenceRate)

                     case List(
                         Right(oppositeWrapper: OppositeToRefRateWrapper[Rate]),
                         Right(referenceWrapper: ReferenceRateWrapper[Rate])
                         ) =>
                       // TODO PR (high) - Dedicated method + tests
//                       TODO PR NEXT - INVESTIGATE AND TEST
                       val mergedRate = TransitiveExchangeRate.rateByTransitivity(oppositeWrapper, referenceWrapper)
                       Right(mergedRate)

                     case _ =>
                       Left(RatesServiceError.OneFrameLookupFailed(s"Currently impossible to calculate this rate."))
                   }
      _ <- Right(println(s"""
           |mergedRate: ${mergedRate}
           |""".stripMargin)) // TODO PR DEBUG
    } yield mergedRate

  private def decodeRateFromCacheOrServiceError(rateAsString: String) = {
    val decodedOrError = decode[Rate](rateAsString)

    def convertError(error: circe.Error): RatesServiceError =
      RatesServiceError.OneFrameDecodeFailed(s"Impossible to extract rate data: ${error.getMessage}")

    decodedOrError.left.map(convertError)
  }

  private def getTransitiveRatesAsStringsFromCache(
      transitivePairs: Seq[TransitiveExchangeRate[CurrenciesPair]]
  ): Option[List[TransitiveExchangeRate[String]]] = {
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
  }

  // TODO PR (low) Test - important, burt requires stub or mock for redis
  private def getReferenceRatesAsStringsFromCache(referencePairWrapper: ReferenceRateWrapper[CurrenciesPair]) =
    getRateFromRedis(key = referencePairWrapper.referenceValue)
      .map(rate => ReferenceRateWrapper(rate))

  // TODO PR (low) Test - important, burt requires stub or mock for redis
  private def getOppositeRefRatesAsStringsFromCache(
      oppositeToRefPairWrapper: OppositeToRefRateWrapper[CurrenciesPair]
  ) =
    getRateFromRedis(key = oppositeToRefPairWrapper.referenceValue)
      .map(rate => OppositeToRefRateWrapper(rate))

  private def oneFrameResponseToRatesOrServiceError(
      decodedResponse: Either[circe.Error, List[Rate]],
  ): Either[RatesServiceError, List[Rate]] =
    decodedResponse.left.map { error =>
      val message = s"""No valid rate found for pair: "${error.getMessage}"""""
      RatesServiceError.OneFrameDecodeFailed(message)
    }

  private def getRateForSameCurrencyPair(currency: Currency): F[RatesServiceError Either Rate] =
    Rate(
      pair = CurrenciesPair.ofSameCurrency(currency),
      price = Price(BigDecimal(1)),
      timestamp = Timestamp.now
    ).asRight[RatesServiceError].pure[F]

}
