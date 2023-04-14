package forex.http.rates

import forex.domain.Currency
import forex.domain.Currency.InvalidCurrencyCode
import org.http4s.{ ParseFailure, QueryParamDecoder }
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher

object QueryParams {

  private def invalidCodeToSafeParseFailure(invalidCode: InvalidCurrencyCode): ParseFailure = {
    val messagePubliclySafe = s""""$invalidCode" is not a supported currency."""
    ParseFailure(
      sanitized = messagePubliclySafe,
      details = messagePubliclySafe
    )
  }

  // TODO PR (low) - verify all objects/classes scopes
  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String]
      .map(Currency.fromString)
      .emap[Currency](_.left.map(invalidCodeToSafeParseFailure))

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("to")

}
