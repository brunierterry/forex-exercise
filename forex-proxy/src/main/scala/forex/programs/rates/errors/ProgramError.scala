package forex.programs.rates.errors

import forex.services.rates.errors.RatesServiceError

sealed trait ProgramError extends Exception {
  def msg: String
}

object ProgramError {
  final case class RateLookupFailed(msg: String) extends ProgramError

  final case class RateDecodeFailed(msg: String) extends ProgramError

  def fromServiceError(serviceError: RatesServiceError): ProgramError = serviceError match {
    case RatesServiceError.OneFrameLookupFailed(msg) => ProgramError.RateLookupFailed(msg)
    case RatesServiceError.OneFrameDecodeFailed(msg) => ProgramError.RateDecodeFailed(msg)
  }
}
