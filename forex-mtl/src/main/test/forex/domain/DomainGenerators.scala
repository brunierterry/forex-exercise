package forex.domain

import org.scalacheck.Gen
import forex.domain.logic._
import forex.domain.logic.TransitiveExchangeRate._
import org.scalacheck.Gen.posNum

import java.time.{ Duration, LocalDateTime, OffsetDateTime, ZoneOffset }
object DomainGenerators {

  /*
  Currency
   */

  val genCurrency: Gen[Currency] =
    Gen.oneOf(Currency.all)

  def genCurrency(excluded: Currency): Gen[Currency] =
    Gen.oneOf(Currency.all.filterNot(_ == excluded))

  val genCurrencyExcludingReference: Gen[Currency] =
    genCurrency(excluded = ReferenceCurrency)

  /*
  CurrenciesPair
   */

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

  /*
  Price
   */

  val genValidPrice: Gen[Price] = {
    Gen.choose(min = BigDecimal(0.0000000000001), max = BigDecimal(Long.MaxValue))
    posNum[BigDecimal].map(Price.apply)
  }

  val gen2ValidPrices: Gen[(Price, Price)] =
    for {
      price1 <- genValidPrice
      price2 <- genValidPrice
    } yield (price1, price2)

  /*
  Timestamp & time related
   */

  val genNonNulDuration: Gen[Duration] =
    Gen
      .choose(
        min = 1L,
        max = Long.MaxValue
      )
      .map(Duration.ofNanos)

  val genNonNulDurationWithMax999Days: Gen[Duration] =
    Gen
      .choose(
        min = 1L,
        max = 99L
      )
      .map(Duration.ofDays)

  val genLocalDate: Gen[OffsetDateTime] =
    Gen
      .choose(
        min = OffsetDateTime.MIN.toEpochSecond,
        max = OffsetDateTime.MAX.toEpochSecond
      )
      .map { epochSeconds =>
        val dateTime: LocalDateTime =
          LocalDateTime.ofEpochSecond(epochSeconds, 0, ZoneOffset.UTC)
        OffsetDateTime.of(dateTime, ZoneOffset.UTC)
      }

  val genTimestamp: Gen[Timestamp] =
    genLocalDate.map(Timestamp.apply)

  /*
  Rate
   */

  val genValidRate: Gen[Rate] =
    for {
      pair <- genCurrenciesPair
      price <- genValidPrice
      timestamp <- genTimestamp
    } yield Rate(pair, price, timestamp)

  val genValidTransitiveWrappedRates: Gen[(OppositeToRefRateWrapper[Rate], ReferenceRateWrapper[Rate])] =
    for {
      currency1 <- genCurrency(excluded = ReferenceCurrency)
      currency2 <- genCurrency(excluded = ReferenceCurrency)
      price1 <- genValidPrice
      price2 <- genValidPrice
      timestamp1 <- genTimestamp
      timestamp2 <- genTimestamp
    } yield
      (
        OppositeToRefRateWrapper(
          Rate(
            pair = CurrenciesPair(ReferenceCurrency, to = currency1),
            price = price1,
            timestamp = timestamp1
          )
        ),
        ReferenceRateWrapper(
          Rate(
            pair = CurrenciesPair(ReferenceCurrency, to = currency2),
            price = price2,
            timestamp = timestamp2
          )
        )
      )

  val gen2ValidRates: Gen[(Rate, Rate)] =
    for {
      rate1 <- genValidRate
      rate2 <- genValidRate
    } yield (rate1, rate2)

  /*
  TransitiveExchangeRate
   */

  def genReferenceRateWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[ReferenceRateWrapper[AsRate]] =
    genReferenceValue.map(ReferenceRateWrapper.apply)

  def genOppositeToRefRateWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[OppositeToRefRateWrapper[AsRate]] =
    genReferenceValue.map(OppositeToRefRateWrapper.apply)

  def genTransitiveExchangeRate[AsRate](genReferenceValue: Gen[AsRate]): Gen[TransitiveExchangeRate[AsRate]] =
    Gen.oneOf(
      genReferenceRateWrapper(genReferenceValue),
      genOppositeToRefRateWrapper(genReferenceValue)
    )

}
