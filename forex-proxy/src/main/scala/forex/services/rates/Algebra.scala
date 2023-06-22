package forex.services.rates

import forex.domain.{ CurrenciesPair, Rate }
import errors._

trait Algebra[F[_]] {
  def getExchangeRate(pair: CurrenciesPair): F[RatesServiceError Either Rate]
}
