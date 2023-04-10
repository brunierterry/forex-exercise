package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
    http: HttpConfig,
    webServices: WebServicesConfig,
)

case class HttpConfig(
    host: String,
    port: Int,
    timeout: FiniteDuration
)
case class WebServicesConfig(
    oneFrame: OneFrameConfig,
)

case class OneFrameConfig(
    host: String,
    port: Int,
)
