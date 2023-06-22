package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ CurrenciesPair, Price, Rate, Timestamp }
import forex.services.rates.errors._

class OneFrameAlwaysHundredStub[F[_]: Applicative] extends Algebra[F] {

  override def getExchangeRate(pair: CurrenciesPair): F[RatesServiceError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[RatesServiceError].pure[F]

}
