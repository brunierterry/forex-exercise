package forex.services.rates.interpreters

import forex.domain.{ CurrenciesPair, Currency, Price, Rate, Timestamp }
import Currency.USD

sealed trait TransitiveExchangeRate[AsRate] {
  def referenceValue: AsRate

  def map[A](f: AsRate => A): TransitiveExchangeRate[A]
}

object TransitiveExchangeRate {

  private val ReferenceCurrency: Currency = USD

  case class ReferenceRateWrapper[AsRate](referenceValue: AsRate) extends TransitiveExchangeRate[AsRate] {
    override def map[A](f: AsRate => A): ReferenceRateWrapper[A] =
      ReferenceRateWrapper(f(referenceValue))
  }

  case class OppositeToRefRateWrapper[AsRate](referenceValue: AsRate) extends TransitiveExchangeRate[AsRate] {
    override def map[A](f: AsRate => A): OppositeToRefRateWrapper[A] =
      OppositeToRefRateWrapper(f(referenceValue))
  }

  // TODO PR (high) test
  // TODO PR (low) remove generic type, as it became useless
  def pairAsTransitiveExchangeRates[AsRate <: CurrenciesPair](pairToExpressAsTransitiveRefs: AsRate)(
      build: (Currency, Currency) => AsRate
  ): List[TransitiveExchangeRate[AsRate]] =
    pairToExpressAsTransitiveRefs match {
      case CurrenciesPair(from, ReferenceCurrency) =>
        List(OppositeToRefRateWrapper(build(ReferenceCurrency, from)))

      case CurrenciesPair(ReferenceCurrency, _) =>
        List(ReferenceRateWrapper(pairToExpressAsTransitiveRefs))

      case CurrenciesPair(from, to) =>
        List(
          OppositeToRefRateWrapper(referenceValue = build(ReferenceCurrency, from)),
          ReferenceRateWrapper(referenceValue = build(ReferenceCurrency, to))
        )
    }

  // TODO PR (high) - Add tests
  def rateByTransitivity(oppositeWrapper: OppositeToRefRateWrapper[Rate],
                         refWrapper: ReferenceRateWrapper[Rate]): Rate =
    Rate(
      pair = CurrenciesPair(from = oppositeWrapper.referenceValue.to, to = refWrapper.referenceValue.to),
      price = priceByTransitivity(oppositeWrapper.map(_.price), refWrapper.map(_.price)),
      timestamp = Timestamp.fromOldest(oppositeWrapper.referenceValue.timestamp, refWrapper.referenceValue.timestamp)
    )

  // TODO PR (low) - consider returning error on price = 0 as it would make no sense
  private def priceByTransitivity(oppositeToRef: OppositeToRefRateWrapper[Price],
                                  reference: ReferenceRateWrapper[Price]): Price = {
    val zero =
      BigDecimal(0)
    val mergedPrice: BigDecimal =
      if (oppositeToRef.referenceValue.value.eq(zero)) zero
      else reference.referenceValue.value / oppositeToRef.referenceValue.value
    Price(mergedPrice)
  }

}
