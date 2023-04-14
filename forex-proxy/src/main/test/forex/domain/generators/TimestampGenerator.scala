package forex.domain.generators

import forex.domain.Timestamp
import org.scalacheck.Gen

import java.time.{Duration, LocalDateTime, OffsetDateTime, ZoneOffset}

object TimestampGenerator {
  val genNonNulDuration: Gen[Duration] =
    Gen
      .choose(
        min = 1L,
        max = Long.MaxValue
      )
      .map(Duration.ofNanos)

  val genLocalDate: Gen[OffsetDateTime] =
    Gen
      .choose(
        min = OffsetDateTime.MIN.toEpochSecond,
        max = OffsetDateTime.MAX.toEpochSecond
      )
      .map { epochSeconds =>
        val dateTime: LocalDateTime =
          LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        OffsetDateTime.of(dateTime, ZoneOffset.UTC)
      }

  val genTimestamp: Gen[Timestamp] =
    genLocalDate.map(Timestamp.apply)
}
