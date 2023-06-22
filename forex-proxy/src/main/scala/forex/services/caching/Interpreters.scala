package forex.services.caching

import forex.config.ApplicationConfig
import forex.services.caching.interpreters.CacheLiveRedis

object Interpreters {
  def live(config: ApplicationConfig): Cache = new CacheLiveRedis(config)
}
