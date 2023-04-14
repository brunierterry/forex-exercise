package forex.domain.generators

import forex.domain.Price
import org.scalacheck.Gen
import org.scalacheck.Gen.posNum

object PriceGenerator {
  val genValidPrice: Gen[Price] = {
    Gen.choose(min = BigDecimal(0.0000000000001), max = BigDecimal(Long.MaxValue))
    posNum[BigDecimal].map(Price.apply)
  }
  val gen2ValidPrices: Gen[(Price, Price)] =
    for {
      price1 <- genValidPrice
      price2 <- genValidPrice
    } yield (price1, price2)
}
