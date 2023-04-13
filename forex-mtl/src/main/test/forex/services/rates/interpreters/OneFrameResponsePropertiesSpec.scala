package forex.services.rates.interpreters

import forex.domain.Rate
import forex.domain.generators.RateGenerator.genValidRate
import io.circe.Json
import io.circe.parser.decode
import org.scalacheck.{ Gen, Properties }
import org.scalacheck.Prop.forAllNoShrink

object OneFrameResponsePropertiesSpec extends Properties("OneFrameResponse") {

  private def jsonFromRate(rate: Rate, bid: String = "500", ask: String = "0.002") =
    Json.obj(
      "from" -> Json.fromString(rate.pair.from.toString),
      "to" -> Json.fromString(rate.pair.to.toString),
      "bid" -> Json.fromString(bid),
      "price" -> Json.fromBigDecimal(rate.price.value),
      "ask" -> Json.fromString(ask),
      "time_stamp" -> Json.fromString(rate.timestamp.value.toString)
    )

  private def oneFrameResponseFromRatesList(rates: List[Rate]): String =
    Json
      .arr(
        rates.map(jsonFromRate(_)): _*
      )
      .toString

  property(
    "rateDecoderFromResponse (used with decode[List[Rate]]) gives valid rates list from a well formed JSON " +
      "containing a list of Rates with expected parameters `from`, `to`, `price`, and `time_stamp`, but also ignored extra parameters"
  ) = forAllNoShrink(Gen.listOf(genValidRate)) { validRates: List[Rate] =>
    val oneFrameResponse =
      oneFrameResponseFromRatesList(validRates)

    import OneFrameResponse.rateDecoderFromResponse
    val decodedRates =
      decode[List[Rate]](oneFrameResponse)

    decodedRates == Right(validRates)
  }
}
