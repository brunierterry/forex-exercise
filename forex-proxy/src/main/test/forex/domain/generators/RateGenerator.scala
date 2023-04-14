package forex.domain.generators

import forex.domain.Rate
import forex.domain.generators.CurrenciesPairGenerator.genCurrenciesPair
import forex.domain.generators.PriceGenerator.genValidPrice
import forex.domain.generators.TimestampGenerator.genTimestamp
import org.scalacheck.Gen

object RateGenerator {
  val genValidRate: Gen[Rate] =
    for {
      pair <- genCurrenciesPair
      price <- genValidPrice
      timestamp <- genTimestamp
    } yield Rate(pair, price, timestamp)

  val gen2ValidRates: Gen[(Rate, Rate)] =
    for {
      rate1 <- genValidRate
      rate2 <- genValidRate
    } yield (rate1, rate2)

}
