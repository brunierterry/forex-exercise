package forex.domain

import io.circe.{ Decoder, Encoder }

case class Rate(
    pair: CurrenciesPair,
    price: Price,
    timestamp: Timestamp
) extends OppositeBuilder[Rate]
    with Timestamp.Freshness {
  override def from: Currency = pair.from

  override def to: Currency = pair.to

  def opposite: Rate =
    copy(
      pair = pair.opposite,
      price = if (price.value.equals(BigDecimal(0.0))) Price(BigDecimal(0.0)) else Price(1 / price.value)
    )
}

object Rate {

  implicit val decodeRate: Decoder[Rate] =
    Decoder.forProduct3("pair", "price", "timestamp")(Rate.apply)

  implicit val encodeRate: Encoder[Rate] =
    Encoder.forProduct3("pair", "price", "timestamp")(rate => (rate.pair, rate.price, rate.timestamp))

}
