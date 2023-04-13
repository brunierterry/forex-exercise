package forex.domain

import forex.domain.generators.TimestampGenerator._
import forex.domain.Timestamp._
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

import java.time.Duration

object TimestampPropertiesSpec extends Properties("Timestamp") {

  property(
    "JSON parsing - timestampEncoder & timestampDecoder : decoding an encoded timestamps gives the same timestamp back"
  ) = forAll(genTimestamp) { timestamp =>
    val encodedAsJson: String = timestamp.asJson(timestampEncoder).toString()
    val decoded               = decode[Timestamp](encodedAsJson)(timestampDecoder)
    decoded == Right(timestamp)
  }

  property("Freshness#moreRecentThan() on same timestamp with nul duration gives false") = forAll(genTimestamp) {
    sameTimestamp: Timestamp =>
      val objectWithFreshness = new Freshness {
        override def timestamp: Timestamp = sameTimestamp
      }
      val isMoreRecent =
        objectWithFreshness.moreRecentThan(
          duration = Duration.ZERO,
          dateTimeReference = sameTimestamp.value
        )
      !isMoreRecent
  }

  property("Freshness#moreRecentThan() on same timestamp with duration > 0 gives true") = forAll(genNonNulDuration) {
    nonNulDuration: Duration =>
      val sameTimestamp =
        Timestamp.now
      val objectWithFreshness = new Freshness {
        override def timestamp: Timestamp = sameTimestamp
      }
      val isMoreRecent =
        objectWithFreshness.moreRecentThan(
          duration = nonNulDuration,
          dateTimeReference = sameTimestamp.value
        )
      isMoreRecent
  }

  property(
    "Freshness#moreRecentThan() compared to oldest datetime but smaller duration param gives true"
  ) = forAll(genNonNulDuration) { nonNulDuration: Duration =>
    val comparedTimestamp =
      Timestamp.now
    val dateTimeReference =
      comparedTimestamp.value.minus(nonNulDuration)
    val objectWithFreshness = new Freshness {
      override def timestamp: Timestamp = comparedTimestamp
    }
    val isMoreRecent =
      objectWithFreshness.moreRecentThan(
        duration = nonNulDuration.minusNanos(1),
        dateTimeReference
      )
    isMoreRecent
  }

  property(
    "Freshness#moreRecentThan() compared to more recent datetime but bigger duration param gives false"
  ) = forAll(genTimestamp) { anyTimestamp =>
    val comparedTimestamp =
      anyTimestamp
    val dateTimeReference =
      anyTimestamp.value.plusDays(2)
    val objectWithFreshness = new Freshness {
      override def timestamp: Timestamp = comparedTimestamp
    }
    val isMoreRecent =
      objectWithFreshness.moreRecentThan(
        duration = Duration.ofDays(1),
        dateTimeReference = dateTimeReference
      )
    !isMoreRecent
  }

  property(
    "fromOldest(sameTimestamp, sameTimestamp) returns sameTimestamp"
  ) = forAll(genTimestamp) { timestamp =>
    fromOldest(timestamp, timestamp) == timestamp
  }

  property(
    "fromOldest(oldTimestamp, recentTimestamp) returns oldTimestamp and parameters order doesnt matters"
  ) = forAll(genTimestamp) { timestamp =>
    val recentTimestamp = timestamp
    val oldTimestamp    = Timestamp(timestamp.value.minusNanos(1L))

    val expected =
      fromOldest(oldTimestamp, recentTimestamp)
    val paramsOrderDoesntMatter =
      fromOldest(recentTimestamp, oldTimestamp) == expected

    expected == oldTimestamp && paramsOrderDoesntMatter
  }

}
