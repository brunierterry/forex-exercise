package forex.domain.generators

import forex.domain.CurrenciesPair
import CurrencyGenerator.genCurrency
import forex.domain.logic.TransitiveExchangeRate.ReferenceCurrency
import org.scalacheck.Gen

object CurrenciesPairGenerator {
  val genCurrenciesPair: Gen[CurrenciesPair] =
    for {
      from <- genCurrency
      to <- genCurrency
    } yield CurrenciesPair(from, to)

  val genSameCurrencyPair: Gen[CurrenciesPair] =
    for {
      currency <- genCurrency
    } yield CurrenciesPair.ofSameCurrency(currency)

  val genReferenceCurrenciesPair: Gen[CurrenciesPair] =
    for {
      to <- genCurrency
    } yield CurrenciesPair(from = ReferenceCurrency, to)
}
