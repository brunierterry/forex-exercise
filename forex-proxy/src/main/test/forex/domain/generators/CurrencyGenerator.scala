package forex.domain.generators

import forex.domain.Currency
import forex.domain.logic.TransitiveReferenceRatesWrapper.ReferenceCurrency
import org.scalacheck.Gen

object CurrencyGenerator {
  val genCurrency: Gen[Currency] =
    Gen.oneOf(Currency.all)

  def genCurrency(excluded: Currency): Gen[Currency] =
    Gen.oneOf(Currency.all.filterNot(_ == excluded))

  val genCurrencyExcludingReference: Gen[Currency] =
    genCurrency(excluded = ReferenceCurrency)

  val genInvalidCurrencyCode: Gen[String] =
    for {
      anyString <- Gen.alphaStr
      allCurrenciesRegex = Currency.all.mkString("|")
    } yield anyString.replaceAll(allCurrenciesRegex, "@@@")
}
