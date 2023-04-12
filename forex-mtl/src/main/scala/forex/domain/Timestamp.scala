package forex.domain

import io.circe.{ Decoder, Encoder, HCursor, Json }

import java.time.{ Duration, OffsetDateTime, ZoneId }
import scala.math.Ordering.Implicits.infixOrderingOps

case class Timestamp(value: OffsetDateTime) extends AnyVal {
  def rawDateTime = value
}

object Timestamp {

  trait Freshness {
    def timestamp: Timestamp

    def moreRecentThan(duration: Duration, dateTimeReference: OffsetDateTime = Timestamp.now.rawDateTime): Boolean =
      timestamp.value.isAfter(dateTimeReference.minus(duration))

  }

  def now: Timestamp =
    Timestamp(OffsetDateTime.now.atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime)

  implicit val decodeTimestamp: Decoder[Timestamp] = new Decoder[Timestamp] {
    final def apply(c: HCursor): Decoder.Result[Timestamp] =
      for {
        value <- c.value.as[OffsetDateTime]
      } yield {
        Timestamp(value)
      }
  }

  implicit val encodeTimestamp: Encoder[Timestamp] = new Encoder[Timestamp] {
    final def apply(timestamp: Timestamp): Json =
      Json.fromString(timestamp.value.toString)
  }

  def fromOldest(a: Timestamp, b: Timestamp): Timestamp =
    Timestamp(a.value min b.value)
}
