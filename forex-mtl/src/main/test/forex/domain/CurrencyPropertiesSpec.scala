package forex.domain

import forex.domain.generators.CurrencyGenerator._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object CurrencyPropertiesSpec extends Properties("Currency") {

  property(
    "JSON parsing - currencyEncoder & currencyDecoder : decoding an encoded currency gives the same currency back"
  ) = forAll(genCurrency) { currency =>
    val encodedAsJson: String = currency.asJson(Currency.currencyEncoder).toString()
    val decoded               = decode[Currency](encodedAsJson)(Currency.currencyDecoder)
    decoded == Right(currency)
  }

  property(
    "fromString(validCurrencyCode) return the currency (wrapped in Right)"
  ) = forAll(genCurrency) { currency =>
    val validCurrencyCode = currency.toString
    Currency.fromString(validCurrencyCode) == Right(currency)
  }

  property(
    "fromString(invalidCurrencyCode) return the invalid code (wrapped in Left)"
  ) = forAll(genInvalidCurrencyCode) { invalidCurrencyCode =>
    Currency.fromString(invalidCurrencyCode) == Left(invalidCurrencyCode)
  }

}
