package forex.domain

import forex.domain.generators.RateGenerator._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

object RatePropertiesSpec extends Properties("Rate") {

  property(
    "JSON parsing - rateEncoder & rateDecoder : decoding an encoded rate gives the same rate back"
  ) = forAll(genValidRate) { rate =>
    val encodedAsJson: String = rate.asJson(Rate.rateEncoder).toString()
    val decoded               = decode[Rate](encodedAsJson)(Rate.rateDecoder)
    decoded == Right(rate)
  }

  property(
    "opposite() : The opposite rate, with valid price, has opposite pair, opposite price but same timestamp"
  ) = forAll(genValidRate) { rate =>
    val expectedOppositeRate =
      Rate(
        pair = rate.pair.opposite,
        price = Price(1 / rate.price.value),
        timestamp = rate.timestamp
      )
    rate.opposite == expectedOppositeRate
  }

  property(
    "opposite() : The opposite of the opposite of a rate is the rate"
  ) = forAll(genValidRate) { rate =>
    val samePair =
      rate.opposite.opposite.pair == rate.pair

    val samePriceWithCalculationApproximation = {
      val smallDifference: BigDecimal = rate.opposite.opposite.price.value - rate.price.value
      Math.abs(smallDifference.toFloat) < 0.000000000000000000001
    }

    val sameTimestamp =
      rate.opposite.opposite.timestamp == rate.timestamp

    samePair && samePriceWithCalculationApproximation && sameTimestamp
  }
}
