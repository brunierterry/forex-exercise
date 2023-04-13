package forex.domain.logic

import forex.domain.generators.CurrenciesPairGenerator._
import forex.domain.generators.CurrencyGenerator._
import forex.domain.generators.PriceGenerator._
import forex.domain.generators.TransitiveExchangeRateGenerator._
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import forex.domain._
import TransitiveExchangeRate._
import org.scalacheck.Gen.posNum

import scala.math.Ordered.orderingToOrdered

object TransitiveExchangeRatePropertiesSpec extends Properties("TransitiveExchangeRate") {

  property("pairAsTransitiveExchangeRates(anyPair) gives a list of 1 or 2 pairs") = forAll(genCurrenciesPair) {
    anyPair: CurrenciesPair =>
      val transitiveRates =
        pairAsTransitiveExchangeRates(anyPair)
      transitiveRates.length == 1 || transitiveRates.length == 2
  }

  property(
    "pairAsTransitiveExchangeRates(sameCurrencyPairExcludingRef) gives a list of 2 pairs wrappers: 1 opposite & 1 ref"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPairExcludingRef =
      CurrenciesPair.ofSameCurrency(notRefCurrency)
    val transitiveRates =
      pairAsTransitiveExchangeRates(sameCurrencyPairExcludingRef)
    val expectedPairs = List(
      OppositeToRefRateWrapper(referenceValue = CurrenciesPair(ReferenceCurrency, to = notRefCurrency)),
      ReferenceRateWrapper(referenceValue = CurrenciesPair(ReferenceCurrency, to = notRefCurrency))
    )
    transitiveRates == expectedPairs
  }

  property(
    "pairAsTransitiveExchangeRates(pairFromReferenceCurrency) gives a list of 1 pair wrapper: 1 ref"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPair =
      CurrenciesPair(from = ReferenceCurrency, to = notRefCurrency)
    val transitiveRates =
      pairAsTransitiveExchangeRates(sameCurrencyPair)
    val expectedPairs = List(
      ReferenceRateWrapper(referenceValue = CurrenciesPair(ReferenceCurrency, to = notRefCurrency))
    )
    transitiveRates == expectedPairs
  }

  property(
    "pairAsTransitiveExchangeRates(pairToReferenceCurrency) gives a list of 1 pair wrapper: 1 opposite"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPair =
      CurrenciesPair(from = notRefCurrency, to = ReferenceCurrency)
    val transitiveRates =
      pairAsTransitiveExchangeRates(sameCurrencyPair)
    val expectedPairs = List(
      OppositeToRefRateWrapper(referenceValue = CurrenciesPair(ReferenceCurrency, to = notRefCurrency))
    )
    transitiveRates == expectedPairs
  }

  property(
    "TransitiveExchangeRate(any).map(identity) gives the same TransitiveExchangeRate"
  ) = forAll(genTransitiveExchangeRate[Int](posNum[Int])) { anyTransitiveExchangeRate: TransitiveExchangeRate[Int] =>
    anyTransitiveExchangeRate.map(identity) == anyTransitiveExchangeRate
  }

  property(
    "priceByTransitivity(price1, price2), on non-nul positive prices, gives a positive result"
  ) = forAll(gen2ValidPrices) {
    case (price1, price2) =>
      val resultPrice =
        priceByTransitivity(OppositeToRefRateWrapper(price1), ReferenceRateWrapper(price2))
      resultPrice.value > BigDecimal(0)
  }

  property(
    "priceByTransitivity(samePrice, samePrice), on non-nul positive prices, gives 1.0"
  ) = forAll(genValidPrice) { samePrice =>
    val resultPrice =
      priceByTransitivity(OppositeToRefRateWrapper(samePrice), ReferenceRateWrapper(samePrice))
    resultPrice.value == BigDecimal(1)
  }

  property(
    "priceByTransitivity(price1, price2), on non-nul positive prices, gives resultPrice = price2 / price1"
  ) = forAll(gen2ValidPrices) {
    case (price1, price2) =>
      val resultPrice =
        priceByTransitivity(OppositeToRefRateWrapper(price1), ReferenceRateWrapper(price2))
      // No approximation needed. The property still valid for division between opposite BigDecimal numbers
      resultPrice.value == (price2.value / price1.value)

  }

  property(
    "rateByTransitivity(rateWrappedAsOppositeRef, rateWrappedAsRef), on rates with non-nul positive prices, gives a rate with correct pair, correct price, and oldest timestamp."
  ) = forAll(genValidTransitiveWrappedRates) {
    case (rateWrappedAsOppositeRef, rateWrappedAsRef) =>
      val mergedRate =
        rateByTransitivity(rateWrappedAsOppositeRef, rateWrappedAsRef)

      val expectedPair = CurrenciesPair(
        from = rateWrappedAsOppositeRef.referenceValue.pair.to,
        to = rateWrappedAsRef.referenceValue.pair.to
      )
      val expectedPrice = // Reuse already proven function
        priceByTransitivity(rateWrappedAsOppositeRef.map(_.price), rateWrappedAsRef.map(_.price))
      val expectedTimestamp =
        if (rateWrappedAsOppositeRef.referenceValue.timestamp.value > rateWrappedAsRef.referenceValue.timestamp.value)
          Timestamp(rateWrappedAsRef.referenceValue.timestamp.value)
        else Timestamp(rateWrappedAsOppositeRef.referenceValue.timestamp.value)

      val expectedRate =
        Rate(
          pair = expectedPair,
          price = expectedPrice,
          timestamp = expectedTimestamp
        )
      mergedRate == expectedRate
  }

}
