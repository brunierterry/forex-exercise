package forex.domain

import forex.domain.DomainGenerators.{genNonNulDuration, genTimestamp}
import forex.domain.Timestamp.Freshness
import org.scalacheck.Prop.forAll
import org.scalacheck.Properties

import java.time.Duration

object TimestampPropertiesSpec extends Properties("Timestamp") {

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

  // TODO PR (high) ADD more tests

}
