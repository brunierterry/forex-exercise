package forex.domain

import io.circe.Decoder
import java.time.OffsetDateTime

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp =
    Timestamp(OffsetDateTime.now)

  implicit val decodeTimestamp: Decoder[Timestamp] =
    Decoder.decodeOffsetDateTime.map(Timestamp.apply)
}
