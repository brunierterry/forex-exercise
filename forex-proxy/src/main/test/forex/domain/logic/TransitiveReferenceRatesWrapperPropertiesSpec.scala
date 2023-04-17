package forex.domain.logic

import forex.domain.generators.CurrencyGenerator._
import forex.domain.generators.PriceGenerator._
import forex.domain.generators.TransitiveReferenceRatesWrapperGenerator._
import org.scalacheck.Properties
import org.scalacheck.Prop.{ forAll, forAllNoShrink }
import forex.domain._
import TransitiveReferenceRatesWrapper._
import forex.domain.generators.RateGenerator.genValidRate
import org.scalacheck.Gen.posNum

import scala.math.Ordered.orderingToOrdered

object TransitiveReferenceRatesWrapperPropertiesSpec extends Properties("TransitiveReferenceRatesWrapper") {

  property(
    "pairAsTransitiveReferenceRates(sameCurrencyPairExcludingRef) gives a list of 2 pairs wrappers: 1 opposite & 1 ref"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPairExcludingRef =
      CurrenciesPair.ofSameCurrency(notRefCurrency)
    val transitiveRates =
      pairAsTransitiveReferenceRates(sameCurrencyPairExcludingRef)
    val expectedPairs = {
      TransitiveRefRatesCouple(
        fromRef = CurrenciesPair(ReferenceCurrency, to = notRefCurrency),
        toRef = CurrenciesPair(ReferenceCurrency, to = notRefCurrency),
      )
    }
    transitiveRates == expectedPairs
  }

  property(
    "pairAsTransitiveReferenceRates(pairFromReferenceCurrency) gives a list of 1 pair wrapper: 1 ref"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPair =
      CurrenciesPair(from = ReferenceCurrency, to = notRefCurrency)
    val transitiveRates =
      pairAsTransitiveReferenceRates(sameCurrencyPair)
    val expectedPairs =
      ReferenceRate(refDirectlyUsed = CurrenciesPair(ReferenceCurrency, to = notRefCurrency))
    transitiveRates == expectedPairs
  }

  property(
    "pairAsTransitiveReferenceRates(pairToReferenceCurrency) gives a list of 1 pair wrapper: 1 opposite"
  ) = forAll(genCurrencyExcludingReference) { notRefCurrency: Currency =>
    val sameCurrencyPair =
      CurrenciesPair(from = notRefCurrency, to = ReferenceCurrency)
    val transitiveRates =
      pairAsTransitiveReferenceRates(sameCurrencyPair)
    val expectedPairs =
      OppositeToRefRate(refUsedAsOpposite = CurrenciesPair(ReferenceCurrency, to = notRefCurrency))
    transitiveRates == expectedPairs
  }

  property(
    "TransitiveReferenceRatesWrapper(any).map(identity) gives the same TransitiveReferenceRatesWrapper"
  ) = forAll(genTransitiveReferenceRatesWrapper[Int](posNum[Int])) { anyTransitiveReferenceRatesWrapper: TransitiveReferenceRatesWrapper[Int] =>
    anyTransitiveReferenceRatesWrapper.map(identity) == anyTransitiveReferenceRatesWrapper
  }

  property(
    "priceByTransitivity(price1, price2), on non-nul positive prices, gives a positive result"
  ) = forAll(gen2ValidPrices) {
    case (price1, price2) =>
      val resultPrice =
        priceByTransitivity(TransitiveRefRatesCouple(price1, price2))
      resultPrice.value > BigDecimal(0)
  }

  property(
    "priceByTransitivity(samePrice, samePrice), on non-nul positive prices, gives 1.0"
  ) = forAll(genValidPrice) { samePrice =>
    val resultPrice =
      priceByTransitivity(TransitiveRefRatesCouple(samePrice, samePrice))
    resultPrice.value == BigDecimal(1)
  }

  property(
    "priceByTransitivity(price1, price2), on non-nul positive prices, gives resultPrice = price2 / price1"
  ) = forAll(gen2ValidPrices) {
    case (price1, price2) =>
      val resultPrice =
        priceByTransitivity(TransitiveRefRatesCouple(price1, price2))
      // No approximation needed. The property still valid for division between opposite BigDecimal numbers
      resultPrice.value == (price2.value / price1.value)

  }

  property(
    "rateByTransitivity(wrapper), on rates with non-nul positive prices, gives a rate with correct pair, correct price, and oldest timestamp."
  ) = forAll(genTransitiveRefRatesCoupleWrapper(genValidRate)) { wrapper =>
    val mergedRate =
      rateByTransitivity(wrapper)

    val expectedPair = CurrenciesPair(
      from = wrapper.fromRef.pair.to,
      to = wrapper.toRef.pair.to
    )
    val expectedPrice = // Reuse already proven function
      priceByTransitivity(wrapper.map(_.price))

    val expectedTimestamp =
      if (wrapper.fromRef.timestamp.value > wrapper.toRef.timestamp.value)
        Timestamp(wrapper.toRef.timestamp.value)
      else Timestamp(wrapper.fromRef.timestamp.value)

    val expectedRate =
      Rate(
        pair = expectedPair,
        price = expectedPrice,
        timestamp = expectedTimestamp
      )
    mergedRate == expectedRate
  }

  property(
    "calculateRateFromReferences(wrapper) decode well-formed encoded rates, and then combine by transitivity"
  ) = forAllNoShrink(genTransitiveReferenceRatesWrapper(genValidRate)) { wrapper =>
    val expectedRate =
      wrapper match {
        case OppositeToRefRate(refUsedAsOpposite: Rate) =>
          refUsedAsOpposite.opposite // `opposite()` already proven in RatePropertiesSpec spec
        case ReferenceRate(refDirectlyUsed: Rate) =>
          refDirectlyUsed
        case ratesCouple: TransitiveRefRatesCouple[Rate] =>
          rateByTransitivity(ratesCouple) // `rateByTransitivity()` already proven above
      }

    calculateRateFromReferences(wrapper) == expectedRate
  }

}
