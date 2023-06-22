package forex.services.caching

import cats.effect.IO
import io.circe.Json

trait Cache {

  def get(key: String): IO[Option[String]]

  def set(key: String, value: Json): IO[Boolean]
}
