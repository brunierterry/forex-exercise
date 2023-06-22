package forex.domain

import forex.domain.CurrenciesPair.PairCode
import io.circe.{ Decoder, Encoder }
import io.circe.generic.semiauto.{ deriveDecoder, deriveEncoder }

trait CurrenciesPairDirection {
  def from: Currency

  def to: Currency

  def pairCode: PairCode =
    from.toString + to.toString

  def isOnSameCurrency: Boolean =
    from == to
}

trait OppositeBuilder[AsRate] extends CurrenciesPairDirection {
  def opposite: AsRate
}

final case class CurrenciesPair(
    from: Currency,
    to: Currency
) extends OppositeBuilder[CurrenciesPair] {
  override def opposite: CurrenciesPair =
    CurrenciesPair(from = to, to = from)
}

object CurrenciesPair {

  type PairCode = String

  implicit val pairDecoder: Decoder[CurrenciesPair] = deriveDecoder
  implicit val pairEncoder: Encoder[CurrenciesPair] = deriveEncoder

  def ofSameCurrency(currency: Currency) =
    CurrenciesPair(currency, currency)
}
