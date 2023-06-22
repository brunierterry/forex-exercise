package forex.services.caching.interpreters

import cats.effect.IO
import com.redis.RedisClient
import forex.config.ApplicationConfig
import forex.services.caching.Cache
import io.circe.Json

class CacheLiveRedis(config: ApplicationConfig) extends Cache {

  private val redisConfig = config.webServices.redis

  // TODO PR (high) - Wrap into a service to mock and test
  // TODO PR (low) - Wrap into a service exposing only useful encapsulated methods
  // TODO PR (low) - refactor and rename to be stack agnostic
  private val redis = new RedisClient(host = redisConfig.host, port = redisConfig.port)

  def get(key: String): IO[Option[String]] =
    IO(
      redis.get[String](key)
    )

  def set(key: String, value: Json): IO[Boolean] =
    IO(
      redis.set(key, value)
    )

}
