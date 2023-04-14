package forex.domain

import forex.domain.generators.CurrenciesPairGenerator._
import forex.domain.generators.CurrencyGenerator.genCurrency
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object CurrenciesPairPropertiesSpec extends Properties("CurrenciesPair") {

  property(
    "JSON parsing - pairEncoder & pairDecoder : decoding an encoded pair gives the same pair back"
  ) = forAll(genCurrenciesPair) { pair =>
    val encodedAsJson: String = pair.asJson(CurrenciesPair.pairEncoder).toString()
    val decoded               = decode[CurrenciesPair](encodedAsJson)(CurrenciesPair.pairDecoder)
    decoded == Right(pair)
  }

  property(
    "opposite() : The opposite pair has from and to values switched"
  ) = forAll(genCurrenciesPair) { pair =>
    pair.opposite == CurrenciesPair(from = pair.to, to = pair.from)
  }

  property(
    "opposite() : The opposite of the opposite of a pair is the pair"
  ) = forAll(genCurrenciesPair) { pair =>
    pair.opposite.opposite == pair
  }

  property(
    "ofSameCurrency(currency) return the same pair as Pair(currency, currency) "
  ) = forAll(genCurrency) { currency =>
    CurrenciesPair.ofSameCurrency(currency) == CurrenciesPair(currency, currency)
  }
}
