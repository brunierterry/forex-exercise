package forex.domain

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {
  final case class Pair(
      from: Currency,
      to: Currency
  ) {
    def isOnSameCurrency: Boolean =
      from == to
  }

  object Pair {
    def ofSameCurrency(currency: Currency) =
      Pair(currency, currency)
  }
}
