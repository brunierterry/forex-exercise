package forex.domain

import forex.domain.generators.PriceGenerator._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object PricePropertiesSpec extends Properties("Price") {

  property(
    "JSON parsing - priceEncoder & priceDecoder : decoding an encoded price gives the same price back"
  ) = forAll(genValidPrice) { price =>
    val encodedAsJson: String = price.asJson(Price.priceEncoder).toString()
    val decoded               = decode[Price](encodedAsJson)(Price.priceDecoder)
    decoded == Right(price)
  }
}
