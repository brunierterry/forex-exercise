package forex.programs.rates

import cats.Functor
import forex.domain._
import forex.services.RatesService
import cats.syntax.functor._
import forex.programs.rates.errors.ProgramError

class Program[F[_]: Functor](
    ratesService: RatesService[F]
) extends Algebra[F] {

  override def getExchangeRate(request: Protocol.GetRatesRequest): F[ProgramError Either Rate] =
    for {
      serviceErrorOrRate <- ratesService.getExchangeRate(CurrenciesPair(request.from, request.to))
      programErrorOrRate = serviceErrorOrRate.left.map(ProgramError.fromServiceError)
    } yield programErrorOrRate

}

object Program {

  def apply[F[_]: Functor](
      ratesService: RatesService[F]
  ): Algebra[F] = new Program[F](ratesService)

}
