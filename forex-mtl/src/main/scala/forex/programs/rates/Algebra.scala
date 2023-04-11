package forex.programs.rates

import forex.domain.Rate
import errors._

trait Algebra[F[_]] {
  def getExchangeRate(request: Protocol.GetRatesRequest): F[ProgramError Either Rate]
}
