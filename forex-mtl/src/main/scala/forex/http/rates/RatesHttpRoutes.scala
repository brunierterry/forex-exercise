package forex.http
package rates

import cats.data.{ EitherT, ValidatedNel }
import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeId
import cats.syntax.flatMap._
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.{ HttpRoutes, ParseFailure, Response }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F]) extends Http4sDsl[F] {

  import Converters._, QueryParams._, Protocol._

  private[http] val prefixPath = "/rates"

  type EitherBadResponseMonad[T] = EitherT[F, F[Response[F]], T]

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(from) +& ToQueryParam(to) =>
      val validResponseOrErrorResponse =
        for {
          fromCurrency <- paramOptionalValidationToRouteMonad(
                           paramValidationOpt = from,
                           missingParamMessage = "Missing \"from\" currency parameter."
                         )
          toCurrency <- paramOptionalValidationToRouteMonad(
                         paramValidationOpt = to,
                         missingParamMessage = "Missing \"to\" currency parameter."
                       )
          ratesRequest = RatesProgramProtocol.GetRatesRequest(fromCurrency, toCurrency)
          rate <- EitherT(rates.get(ratesRequest)).leftMap { programError =>
                   BadRequest(programError.msg)
                 }
        } yield Ok(rate.asGetApiResponse)

      validResponseOrErrorResponse.value.flatMap {
        case Right(successResponse) => successResponse
        case Left(failureResponse)  => failureResponse
      }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )

  private def paramValidationToEitherParseFailure[T](
      paramValidation: ValidatedNel[ParseFailure, T]
  ): Either[ParseFailure, T] =
    paramValidation.toEither.left.map(_.head)

  private def eitherToResponseEitherMonad[E, T](
      valueOrError: Either[E, T]
  )(makeResponse: E => F[Response[F]]): EitherBadResponseMonad[T] = {
    val validParamOrBadRequest = valueOrError.left.map(makeResponse)
    EitherT(validParamOrBadRequest.pure[F])
  }

  private def paramValidationToRouteMonad[T](
      paramValidation: ValidatedNel[ParseFailure, T]
  ): EitherBadResponseMonad[T] = {
    val validParamOrParseFailure = paramValidationToEitherParseFailure(paramValidation)
    eitherToResponseEitherMonad(validParamOrParseFailure) {
      case ParseFailure(sanitizedMessage, _) =>
        BadRequest(sanitizedMessage)
    }
  }
  private def paramOptionalValidationToRouteMonad[T](
      paramValidationOpt: Option[ValidatedNel[ParseFailure, T]],
      missingParamMessage: String
  ): EitherBadResponseMonad[T] =
    paramValidationOpt
      .map(paramValidationToRouteMonad)
      .getOrElse(
        EitherT.leftT(BadRequest(missingParamMessage))
      )

}
