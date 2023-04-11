package forex.domain

import io.circe.{ Decoder, Encoder, HCursor, Json }

case class Price(value: BigDecimal) extends AnyVal

object Price {
  def apply(value: Integer): Price =
    Price(BigDecimal(value))

  implicit val decodePrice: Decoder[Price] = new Decoder[Price] {
    final def apply(c: HCursor): Decoder.Result[Price] =
      for {
        value <- c.value.as[BigDecimal]
      } yield {
        Price(value)
      }
  }

  implicit val encodePrice: Encoder[Price] = new Encoder[Price] {
    final def apply(price: Price): Json =
      Json.fromString(price.value.toString)
  }
}
