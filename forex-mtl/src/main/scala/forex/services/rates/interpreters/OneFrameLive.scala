package forex.services.rates.interpreters

import cats.Applicative
import cats.effect.IO
import cats.syntax.applicative._
import cats.syntax.either._
import forex.config.ApplicationConfig
import forex.domain.{ Currency, Price, Rate, Timestamp }
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

class OneFrameLive[F[_]: Applicative](config: ApplicationConfig) extends Algebra[F] {

  implicit private val cs: ContextShift[IO] = IO.contextShift(global)

  private val blockingPool           = Executors.newFixedThreadPool(5)
  private val blocker                = Blocker.liftExecutorService(blockingPool)
  private val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

  private val oneFrameConfig = config.webServices.oneFrame

  def request(pair: Rate.Pair) = GET(
    // TODO PR (low) - consider to use https
    Uri.unsafeFromString(
      s"http://${oneFrameConfig.host}:${oneFrameConfig.port}/rates?pair=${uriFormat(pair)}"
    ),
    Header.Raw(
      name = CaseInsensitiveString("token"),
      value = "10dc303535874aeccc86a8251e6992f5"
    ),
    Accept(MediaType.application.json)
  )

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    if (pair.isOnSameCurrency) {
      getRateForSameCurrencyPair(currency = pair.from)
    } else {
      getRateForDifferentCurrenciesPair(pair)
    }

  // TODO PR (high) - Implement fetching from cache + refreshing on too old
  // TODO PR (low) - Implement refreshing in a different fiber
  private def getRateForDifferentCurrenciesPair(pair: Rate.Pair): F[RatesServiceError Either Rate] = {
    val preparedRequest = httpClient.expect[String](request(pair))

    // TODO PR (low) - find a more elegant way to lift IO as an Applicative F
    val results = preparedRequest.unsafeRunSync()

    import OneFrameResponse.decodeRateFromResponse
    val decodedResponse: Either[circe.Error, List[Rate]] = decode[List[Rate]](results)

    oneFrameResponseToRateOrServiceError(pair, decodedResponse).pure[F]
  }

  private def oneFrameResponseToRateOrServiceError(
      pair: Rate.Pair,
      decodedResponse: Either[circe.Error, List[Rate]],
  ): Either[RatesServiceError, Rate] =
    decodedResponse match {
      case Right(foundRate :: _) =>
        Right(foundRate)
      // TODO PR (mid) - Handle server internal error, to display a message in response.
      case _ =>
        val message = s"""No valid rate found for pair: "${uriFormat(pair)}"""""
        Left(RatesServiceError.OneFrameDecodeFailed(message))
    }

  private def uriFormat(pair: Rate.Pair): String =
    pair.from.toString + pair.to.toString

  private def getRateForSameCurrencyPair(currency: Currency): F[RatesServiceError Either Rate] =
    Rate(
      pair = Rate.Pair.ofSameCurrency(currency),
      price = Price(BigDecimal(1)),
      timestamp = Timestamp.now
    ).asRight[RatesServiceError].pure[F]

}
