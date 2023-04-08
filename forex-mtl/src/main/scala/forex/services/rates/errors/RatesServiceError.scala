package forex.services.rates.errors

sealed trait RatesServiceError
object RatesServiceError {
  final case class OneFrameLookupFailed(msg: String) extends RatesServiceError
}
