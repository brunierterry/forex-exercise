package forex.services.rates.interpreters

import forex.domain.Rate
import forex.domain.generators.RateGenerator.genValidRate
import forex.domain.generators.TransitiveExchangeRateGenerator.{
  genOppositeToRefRateWrapper,
  genReferenceRateWrapper,
  genTransitiveExchangeRate,
  genValidTransitiveWrappedRates
}
import forex.domain.logic.TransitiveExchangeRate
import forex.domain.logic.TransitiveExchangeRate._
import forex.services.rates.errors.RatesServiceError
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.{ forAll, forAllNoShrink }
import org.scalacheck.{ Gen, Properties }
import forex.services.rates.interpreters.OneFrameLive._

// TODO PR (high) improve testing by improving dependencies management
object OneFrameLivePropertiesSpec extends Properties("OneFrameLive") {

  private val decodingError: RatesServiceError =
    RatesServiceError.OneFrameDecodeFailed("Decoding has failed")

  private val genOneFrameDecodeFailed: Gen[RatesServiceError] =
    Gen.const(decodingError)

  private val genErrorOrWrapper: Gen[Either[RatesServiceError, TransitiveExchangeRate[Rate]]] =
    Gen.either(genOneFrameDecodeFailed, genTransitiveExchangeRate(genValidRate))

  private val genListContainingError: Gen[List[Either[RatesServiceError, TransitiveExchangeRate[Rate]]]] =
    for {
      listOfErrorsAndWrappers <- Gen.nonEmptyListOf(genErrorOrWrapper)
      positionToReplaceByError <- Gen.choose(0, listOfErrorsAndWrappers.size - 1)
    } yield listOfErrorsAndWrappers.updated(positionToReplaceByError, Left(decodingError))

  private val genInvalidListOf2TransitiveExchangeRateWrappers
    : Gen[List[Either[RatesServiceError, TransitiveExchangeRate[Rate]]]] =
    for {
      refRateWrapper1 <- genReferenceRateWrapper(genValidRate)
      refRateWrapper2 <- genReferenceRateWrapper(genValidRate)
      oppositeWrapper1 <- genOppositeToRefRateWrapper(genValidRate)
      oppositeWrapper2 <- genOppositeToRefRateWrapper(genValidRate)
      invalidCombinationsOf2Wrappers <- Gen.oneOf(
                                         Gen.const(List(Right(oppositeWrapper1), Right(oppositeWrapper2))),
                                         Gen.const(List(Right(refRateWrapper1), Right(refRateWrapper2))),
                                         Gen.const(List(Right(refRateWrapper1), Right(oppositeWrapper2))),
                                       )
    } yield invalidCombinationsOf2Wrappers

  private val genValidRateAndOneRateJsonAttributeOrSubAttribute =
    for {
      validRate <- genValidRate
      attributeKey <- Gen.oneOf("pair", "to", "from", "price", "timestamp")
    } yield (validRate, attributeKey)

  private def encodeRatesInTransitivityWrappers(
      wrappersOnValidRates: List[TransitiveExchangeRate[Rate]]
  ): List[TransitiveExchangeRate[String]] =
    wrappersOnValidRates.map(wrapper => wrapper.map(rate => rate.asJson.toString))

  property(
    "mergeErrorsOrRates(emptyList) on empty list gives an error"
  ) = forAll(Gen.const(List.empty[Either[RatesServiceError, TransitiveExchangeRate[Rate]]])) { emptyList =>
    val expectedError =
      Left(RatesServiceError.OneFrameLookupFailed(s"Currently impossible to calculate this rate."))
    mergeErrorsOrRates(emptyList) == expectedError
  }

  property(
    "mergeErrorsOrRates(listContainingAtLeastOneError) on a list containing at list one error gives an error"
  ) = forAllNoShrink(genListContainingError) { listContainingAtLeastOneError =>
    val hasRealisticDecodingError =
      listContainingAtLeastOneError.take(2).exists(_.isLeft)

    val expectedError =
      if (hasRealisticDecodingError) Left(decodingError)
      else Left(RatesServiceError.OneFrameLookupFailed(s"Currently impossible to calculate this rate."))

    mergeErrorsOrRates(listContainingAtLeastOneError) == expectedError
  }

  property(
    "mergeErrorsOrRates(invalidListOf2Wrappers) on a list containing at list one error gives an error"
  ) = forAllNoShrink(genInvalidListOf2TransitiveExchangeRateWrappers) { invalidListOf2Wrappers =>
    val expectedError =
      Left(RatesServiceError.OneFrameLookupFailed(s"Currently impossible to calculate this rate."))

    mergeErrorsOrRates(invalidListOf2Wrappers) == expectedError
  }

  property(
    "mergeErrorsOrRates(singleOppositeRefWrapperOnRefRateA) on a list containing " +
      "a single reference rate `A` wrapped as Opposite Ref (i.e. the ref rate `A` is the opposite of desired rate) " +
      "give the opposite rate of `A`."
  ) = forAllNoShrink(genValidRate) { referenceRateA =>
    val singleOppositeRefWrapperOnRefRateA =
      List(Right(OppositeToRefRateWrapper(referenceRateA)))
    val result =
      mergeErrorsOrRates(singleOppositeRefWrapperOnRefRateA)

    result == Right(referenceRateA.opposite) // `opposite` method is PBT proven in dedicated spec.
  }

  property(
    "mergeErrorsOrRates(singleReferenceWrapperOnRefRateB) on a list containing " +
      "a single reference rate `B` wrapped as Reference (i.e. the ref rate `B` is the desired rate) " +
      "give the rate `B`."
  ) = forAllNoShrink(genValidRate) { referenceRateB =>
    val singleReferenceWrapperOnRefRateB =
      List(Right(ReferenceRateWrapper(referenceRateB)))
    val result =
      mergeErrorsOrRates(singleReferenceWrapperOnRefRateB)

    result == Right(referenceRateB)
  }

  property(
    "mergeErrorsOrRates(List(oppositeRefWrapperOnRefRateA, referenceWrapperOnRefRateB)) on a list containing, " +
      "first, a reference rate `A` wrapped as Opposite Ref (i.e. the ref rate `A` is the opposite of desired rate), " +
      "then, a reference rate `B` wrapped as Reference (i.e. the ref rate `B` is the desired rate) " +
      "give a new rate C, which is the valid transitive combination of rates `A` and `B`."
  ) = forAllNoShrink(genValidTransitiveWrappedRates) {
    case (rateWrappedAsOppositeRef, rateWrappedAsRef) =>
      val result =
        mergeErrorsOrRates(List(Right(rateWrappedAsOppositeRef), Right(rateWrappedAsRef)))

      val expectedRate: Either[RatesServiceError, Rate] =
        // `rateByTransitivity` function is PBT proven in dedicated spec.
        Right(rateByTransitivity(rateWrappedAsOppositeRef, rateWrappedAsRef))

      result == expectedRate
  }

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
    "decodeWrappedTransitiveEncodedRates(wrappersOnValidRates) decode well-formed encoded rates, wrapped in same order"
  ) = forAllNoShrink(Gen.nonEmptyListOf(genTransitiveExchangeRate(genValidRate))) {
    case wrappersOnValidRates =>
      val encodedRates =
        encodeRatesInTransitivityWrappers(wrappersOnValidRates)

      val expectedResult =
        wrappersOnValidRates.map(Right.apply)

      decodeWrappedTransitiveEncodedRates(encodedRates) == expectedResult
  }

  property(
    "decodeAndMergeRates(wrappersOnValidRates) decode well-formed encoded rates, and then combine by transitivity"
  ) = forAllNoShrink(genValidTransitiveWrappedRates) {
    case (rateWrappedAsOppositeRef, rateWrappedAsRef) =>
      val encodedRates =
        encodeRatesInTransitivityWrappers(List(rateWrappedAsOppositeRef, rateWrappedAsRef))

      val errorOrValidTransitivityWrappers: Either[RatesServiceError, List[TransitiveExchangeRate[String]]] =
        Right(encodedRates)

      val result =
        decodeAndMergeRates(errorOrValidTransitivityWrappers)

      // The expected result is an approximation as encoding and re-decoding a Rate change the precision.
      // However, results are still consistent in normal workflow, as all rates are decoded from the cache.
      val approxExpectedRate: Rate =
        // `rateByTransitivity` function is PBT proven in dedicated spec.
        rateByTransitivity(rateWrappedAsOppositeRef, rateWrappedAsRef)

      val sameAsExpectedPair: Boolean =
        result.toOption.exists(_.pair == approxExpectedRate.pair)
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
