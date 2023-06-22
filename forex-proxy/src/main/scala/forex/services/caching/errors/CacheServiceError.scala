package forex.services.caching.errors

sealed trait CacheServiceError

object CacheServiceError {

  final case class ValueNotFoundError(msg: String) extends CacheServiceError
}
