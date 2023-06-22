package forex.services.rates

import cats.Applicative
import forex.config.ApplicationConfig
import interpreters._

object Interpreters {
  def alwaysHundredStub[F[_]: Applicative]: Algebra[F]               = new OneFrameAlwaysHundredStub[F]
  def live[F[_]: Applicative](config: ApplicationConfig): Algebra[F] = new OneFrameLive[F](config)
}
