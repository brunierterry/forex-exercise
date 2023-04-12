package forex.domain.logic

import forex.domain.Currency.USD
import forex.domain._

sealed trait TransitiveExchangeRate[AsRate] {
  def referenceValue: AsRate

  def map[A](f: AsRate => A): TransitiveExchangeRate[A]
}

object TransitiveExchangeRate {

  private[domain] val ReferenceCurrency: Currency = USD

  def makeReferenceCurrenciesPair(currency: Currency) =
    CurrenciesPair(ReferenceCurrency, currency)

  case class ReferenceRateWrapper[AsRate](referenceValue: AsRate) extends TransitiveExchangeRate[AsRate] {
    override def map[A](f: AsRate => A): ReferenceRateWrapper[A] =
      ReferenceRateWrapper(f(referenceValue))
  }

  case class OppositeToRefRateWrapper[AsRate](referenceValue: AsRate) extends TransitiveExchangeRate[AsRate] {
    override def map[A](f: AsRate => A): OppositeToRefRateWrapper[A] =
      OppositeToRefRateWrapper(f(referenceValue))
  }

  def pairAsTransitiveExchangeRates(
      pairToExpressAsTransitiveRefs: CurrenciesPair
  ): List[TransitiveExchangeRate[CurrenciesPair]] =
    pairToExpressAsTransitiveRefs match {
      case CurrenciesPair(from, ReferenceCurrency) =>
        List(OppositeToRefRateWrapper(makeReferenceCurrenciesPair(currency = from)))

      case CurrenciesPair(ReferenceCurrency, _) =>
        List(ReferenceRateWrapper(pairToExpressAsTransitiveRefs))

      case CurrenciesPair(from, to) =>
        List(
          OppositeToRefRateWrapper(referenceValue = makeReferenceCurrenciesPair(currency = from)),
          ReferenceRateWrapper(referenceValue = makeReferenceCurrenciesPair(currency = to))
        )
    }

  def rateByTransitivity(oppositeWrapper: OppositeToRefRateWrapper[Rate],
                         refWrapper: ReferenceRateWrapper[Rate]): Rate =
    Rate(
      pair = CurrenciesPair(from = oppositeWrapper.referenceValue.to, to = refWrapper.referenceValue.to),
      price = priceByTransitivity(oppositeWrapper.map(_.price), refWrapper.map(_.price)),
      timestamp = Timestamp.fromOldest(oppositeWrapper.referenceValue.timestamp, refWrapper.referenceValue.timestamp)
    )

  // TODO PR (low) - consider returning error on price <= 0 as it would make no sense & consider as error if returned by the cache or OneFrame
  private[domain] def priceByTransitivity(oppositeToRef: OppositeToRefRateWrapper[Price],
                                          reference: ReferenceRateWrapper[Price]): Price = {
    val zero =
      BigDecimal(0)
    val mergedPrice: BigDecimal =
      if (oppositeToRef.referenceValue.value == reference.referenceValue.value) BigDecimal(1)
      else if (oppositeToRef.referenceValue.value == zero) zero
      else reference.referenceValue.value / oppositeToRef.referenceValue.value
    Price(mergedPrice)
  }

}
