package forex.services.rates.interpreters

import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Currency, Price, Rate, Timestamp }
import forex.services.rates.Algebra
import forex.services.rates.errors._

class OneFrameLive[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    if (pair.hasSameCurrency) {
      sameCurrencyPairRate(currency = pair.from)
    } else {
      differentCurrenciesPairRate(pair)
    }

  // TODO PR (top - TODO NEXT) - Naive implementation querying One-Frame directly
  // TODO PR (high) - Implement fetching from cache
  private def differentCurrenciesPairRate(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[RatesServiceError].pure[F]

  private def sameCurrencyPairRate(currency: Currency): F[RatesServiceError Either Rate] =
    Rate(
      pair = Rate.Pair.ofSameCurrency(currency),
      price = Price(BigDecimal(1)),
      timestamp = Timestamp.now
    ).asRight[RatesServiceError].pure[F]

}
