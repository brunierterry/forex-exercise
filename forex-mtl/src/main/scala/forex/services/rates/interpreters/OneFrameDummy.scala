package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors._

// TODO PR (low) - consider renaming as stub amd moving
class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[RatesServiceError Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[RatesServiceError].pure[F]

}
