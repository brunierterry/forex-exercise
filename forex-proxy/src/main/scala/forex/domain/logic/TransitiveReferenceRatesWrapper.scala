package forex.domain.logic

import forex.domain.Currency.USD
import forex.domain._

sealed trait TransitiveReferenceRatesWrapper[AsRefRate] {
  def map[A](f: AsRefRate => A): TransitiveReferenceRatesWrapper[A]

}

object TransitiveReferenceRatesWrapper {

  private[domain] val ReferenceCurrency: Currency = USD

  private def makeReferenceCurrenciesPair(currency: Currency) =
    CurrenciesPair(ReferenceCurrency, currency)

  case class OppositeToRefRate[AsRefRate](refUsedAsOpposite: AsRefRate)
      extends TransitiveReferenceRatesWrapper[AsRefRate] {
    override def map[A](f: AsRefRate => A): OppositeToRefRate[A] =
      OppositeToRefRate(f(refUsedAsOpposite))
  }

  case class ReferenceRate[AsRefRate](refDirectlyUsed: AsRefRate) extends TransitiveReferenceRatesWrapper[AsRefRate] {
    override def map[A](f: AsRefRate => A): ReferenceRate[A] =
      ReferenceRate(f(refDirectlyUsed))
  }

  case class TransitiveRefRatesCouple[AsRefRate](
      fromRef: AsRefRate,
      toRef: AsRefRate,
  ) extends TransitiveReferenceRatesWrapper[AsRefRate] {
    override def map[A](f: AsRefRate => A): TransitiveRefRatesCouple[A] =
      TransitiveRefRatesCouple(
        fromRef = f(fromRef),
        toRef = f(toRef),
      )
  }

  def pairAsTransitiveReferenceRates(
      pairToExpressAsTransitiveRefs: CurrenciesPair
  ): TransitiveReferenceRatesWrapper[CurrenciesPair] =
    pairToExpressAsTransitiveRefs match {
      case CurrenciesPair(from, ReferenceCurrency) =>
        OppositeToRefRate(makeReferenceCurrenciesPair(currency = from))

      case CurrenciesPair(ReferenceCurrency, _) =>
        ReferenceRate(pairToExpressAsTransitiveRefs)

      case CurrenciesPair(from, to) =>
        TransitiveRefRatesCouple(
          fromRef = makeReferenceCurrenciesPair(currency = from),
          toRef = makeReferenceCurrenciesPair(currency = to)
        )
    }

  def calculateRateFromReferences(wrapper: TransitiveReferenceRatesWrapper[Rate]): Rate =
    wrapper match {
      case OppositeToRefRate(refUsedAsOpposite) =>
        refUsedAsOpposite.opposite

      case ReferenceRate(refDirectlyUsed) =>
        refDirectlyUsed

      case couple: TransitiveRefRatesCouple[Rate] =>
        rateByTransitivity(couple)
    }

  def rateByTransitivity(ratesCouple: TransitiveRefRatesCouple[Rate]) =
    Rate(
      pair = CurrenciesPair(from = ratesCouple.fromRef.to, to = ratesCouple.toRef.to),
      price = priceByTransitivity(ratesCouple.map(_.price)),
      timestamp = Timestamp.fromOldest(ratesCouple.fromRef.timestamp, ratesCouple.toRef.timestamp)
    )

  // TODO PR (low) - consider returning error on price <= 0 as it would make no sense & consider as error if returned by the cache or OneFrame
  private[domain] def priceByTransitivity(pricesCouple: TransitiveRefRatesCouple[Price]): Price = {
    val zero =
      BigDecimal(0)
    val mergedPrice: BigDecimal =
      if (pricesCouple.fromRef.value == pricesCouple.toRef.value) BigDecimal(1)
      else if (pricesCouple.fromRef.value == zero) zero
      else pricesCouple.toRef.value / pricesCouple.fromRef.value
    Price(mergedPrice)
  }

}
