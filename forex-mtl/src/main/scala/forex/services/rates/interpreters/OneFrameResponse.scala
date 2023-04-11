package forex.services.rates.interpreters

import forex.domain.CurrenciesPair
import forex.domain.{ Currency, Price, Rate, Timestamp }
import io.circe.{ Decoder, HCursor }

import java.time.OffsetDateTime

object OneFrameResponse {

  implicit val decodeRateFromResponse: Decoder[Rate] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[Currency]
      to <- c.downField("to").as[Currency]
      price <- c.downField("price").as[BigDecimal]
      timestamp <- c.downField("time_stamp").as[OffsetDateTime]
    } yield
      Rate(
        pair = CurrenciesPair(from, to),
        price = Price(price),
        timestamp = Timestamp(timestamp)
    )

}
