package forex.domain.generators

import forex.domain.generators.PriceGenerator.genValidPrice
import forex.domain.{ CurrenciesPair, Rate }
import forex.domain.generators.TimestampGenerator.genTimestamp
import forex.domain.logic.TransitiveExchangeRate
import forex.domain.logic.TransitiveExchangeRate.{ OppositeToRefRateWrapper, ReferenceCurrency, ReferenceRateWrapper }
import org.scalacheck.Gen

object TransitiveExchangeRateGenerator {
  def genOppositeToRefRateWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[OppositeToRefRateWrapper[AsRate]] =
    genReferenceValue.map(OppositeToRefRateWrapper.apply)

  def genReferenceRateWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[ReferenceRateWrapper[AsRate]] =
    genReferenceValue.map(ReferenceRateWrapper.apply)

  def genTransitiveExchangeRate[AsRate](genReferenceValue: Gen[AsRate]): Gen[TransitiveExchangeRate[AsRate]] =
    Gen.oneOf(
      TransitiveExchangeRateGenerator.genReferenceRateWrapper(genReferenceValue),
      TransitiveExchangeRateGenerator.genOppositeToRefRateWrapper(genReferenceValue)
    )

  val genValidTransitiveWrappedRates: Gen[(OppositeToRefRateWrapper[Rate], ReferenceRateWrapper[Rate])] =
    for {
      currency1 <- CurrencyGenerator.genCurrency(excluded = ReferenceCurrency)
      currency2 <- CurrencyGenerator.genCurrency(excluded = ReferenceCurrency)
      price1 <- genValidPrice
      price2 <- genValidPrice
      timestamp1 <- genTimestamp
      timestamp2 <- genTimestamp
    } yield
      (
        OppositeToRefRateWrapper(
          Rate(
            pair = CurrenciesPair(ReferenceCurrency, to = currency1),
            price = price1,
            timestamp = timestamp1
          )
        ),
        ReferenceRateWrapper(
          Rate(
            pair = CurrenciesPair(ReferenceCurrency, to = currency2),
            price = price2,
            timestamp = timestamp2
          )
        )
      )

  def genInvalidListOf2TransitiveExchangeRateWrappers[AsRate](
      genReferenceValue: Gen[AsRate]
  ): Gen[List[TransitiveExchangeRate[AsRate]]] =
    for {
      refRateWrapper1 <- genReferenceRateWrapper(genReferenceValue)
      refRateWrapper2 <- genReferenceRateWrapper(genReferenceValue)
      oppositeWrapper1 <- genOppositeToRefRateWrapper(genReferenceValue)
      oppositeWrapper2 <- genOppositeToRefRateWrapper(genReferenceValue)
      invalidCombinationsOf2Wrappers <- Gen.oneOf(
                                         Gen.const(List(oppositeWrapper1, oppositeWrapper2)),
                                         Gen.const(List(refRateWrapper1, refRateWrapper2)),
                                         Gen.const(List(refRateWrapper1, oppositeWrapper2)),
                                       )
    } yield invalidCombinationsOf2Wrappers

}
