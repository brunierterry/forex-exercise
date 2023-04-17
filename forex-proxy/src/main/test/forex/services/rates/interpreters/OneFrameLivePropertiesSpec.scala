package forex.services.rates.interpreters

import forex.domain.Rate
import forex.domain.generators.RateGenerator.genValidRate
import forex.domain.generators.TransitiveReferenceRatesWrapperGenerator._
import forex.domain.logic.TransitiveReferenceRatesWrapper
import forex.domain.logic.TransitiveReferenceRatesWrapper._
import forex.services.rates.errors.RatesServiceError
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAllNoShrink
import org.scalacheck.{ Gen, Properties }
import forex.services.rates.interpreters.OneFrameLive._

// TODO PR (high) improve testing by improving dependencies management
object OneFrameLivePropertiesSpec extends Properties("OneFrameLive") {

  private val genValidRateAndOneRateJsonAttributeOrSubAttribute =
    for {
      validRate <- genValidRate
      attributeKey <- Gen.oneOf("pair", "to", "from", "price", "timestamp")
    } yield (validRate, attributeKey)

  private def encodeRatesInTransitivityWrappers(
      wrappersOnValidRates: TransitiveReferenceRatesWrapper[Rate]
  ): TransitiveReferenceRatesWrapper[String] =
    wrappersOnValidRates.map(rate => rate.asJson.toString)

  property(
    "rateDecoderFromCacheOrServiceError(validRateEncodedAsString) on a valid encoded Rate " +
      "gives the decoded rate."
  ) = forAllNoShrink(genValidRate) { validRate =>
    val encodedRate: String =
      validRate.asJson(Rate.rateEncoder).toString

    rateDecoderFromCacheOrServiceError(encodedRate) == Right(validRate)
  }

  property(
    "rateDecoderFromCacheOrServiceError(validRateEncodedAsString) on va valid encoded Rate " +
      "gives the decoded rate."
  ) = forAllNoShrink(genValidRateAndOneRateJsonAttributeOrSubAttribute) {
    case (validRate, attributeKey) =>
      val encodedRate: String =
        validRate.asJson(Rate.rateEncoder).toString
      val invalidEncodedRate =
        encodedRate.replace(attributeKey, "a")

      val additionalFailedFieldOnSubAttributeError =
        if (Set("from", "to").contains(attributeKey)) s",DownField(pair)" else ""
      val expectedError =
        Left(
          RatesServiceError.OneFrameDecodeFailed(
            s"Impossible to extract rate data: Attempt to decode value on failed cursor: DownField($attributeKey)$additionalFailedFieldOnSubAttributeError"
          )
        )

      rateDecoderFromCacheOrServiceError(invalidEncodedRate) == expectedError
  }

  property(
    "decodeWrappedTransitiveEncodedRates(wrapper) decode well-formed encoded rates, wrapped in same order"
  ) = forAllNoShrink(genTransitiveReferenceRatesWrapper(genValidRate)) { wrapper =>
    val encodedRates =
      encodeRatesInTransitivityWrappers(wrapper)

    decodeWrappedTransitiveEncodedRates(encodedRates) == Right(wrapper)
  }

  property(
    "decodeAndMergeRates(wrappersOnValidRates) decode well-formed encoded rates, and then combine by transitivity"
  ) = forAllNoShrink(genTransitiveReferenceRatesWrapper(genValidRate)) { wrapper =>
    val encodedRates =
      encodeRatesInTransitivityWrappers(wrapper)

    val errorOrValidTransitivityWrappers: Either[RatesServiceError, TransitiveReferenceRatesWrapper[String]] =
      Right(encodedRates)

    val result =
      decodeAndMergeRates(errorOrValidTransitivityWrappers)

    // The expected result is an approximation as encoding and re-decoding a Rate change the precision.
    // However, results are still consistent in normal workflow, as all rates are decoded from the cache.
    val approxExpectedRate: Rate =
      // `calculateRateFromReferences` function is PBT proven in dedicated spec.
      calculateRateFromReferences(wrapper)

    val sameAsExpectedPair: Boolean = {
      result.toOption.exists(_.pair == approxExpectedRate.pair)
    }
    val approximatelySameAsExpectedPrice: Boolean =
      result.toOption.exists { resultRate =>
        val smallDifference =
          (resultRate.price.value - approxExpectedRate.price.value).toFloat
        Math.abs(smallDifference) < 0.00000000001
      }
    val sameAsExpectedTimestamp: Boolean =
      result.toOption.exists(_.timestamp == approxExpectedRate.timestamp)

    sameAsExpectedPair &&
    approximatelySameAsExpectedPrice &&
    sameAsExpectedTimestamp
  }

}
