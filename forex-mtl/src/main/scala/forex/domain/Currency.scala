package forex.domain

import cats.Show
import io.circe.{ Decoder, Encoder, Json }

sealed trait Currency

object Currency {

  type InvalidCurrencyCode = String

  implicit val show: Show[Currency] =
    Show.show(_.toString)

  implicit val decodeCurrency: Decoder[Currency] =
    Decoder.decodeString.emap(fromString)

  implicit val encodeCurrency: Encoder[Currency] = new Encoder[Currency] {
    final def apply(currency: Currency): Json =
      Json.fromString(currency.toString)
  }

  val all = List(
    AED,
    AFN,
    ALL,
    AMD,
    ANG,
    AOA,
    ARS,
    AUD,
    AWG,
    AZN,
    BAM,
    BBD,
    BDT,
    BGN,
    BHD,
    BIF,
    BMD,
    BND,
    BOB,
    BRL,
    BSD,
    BTN,
    BWP,
    BYN,
    BZD,
    CAD,
    CDF,
    CHF,
    CLP,
    CNY,
    COP,
    CRC,
    CUC,
    CUP,
    CVE,
    CZK,
    DJF,
    DKK,
    DOP,
    DZD,
    EGP,
    ERN,
    ETB,
    EUR,
    FJD,
    FKP,
    GBP,
    GEL,
    GHS,
    GIP,
    GMD,
    GNF,
    GTQ,
    GYD,
    HKD,
    HNL,
    HTG,
    HUF,
    IDR,
    ILS,
    INR,
    IQD,
    IRR,
    ISK,
    JMD,
    JOD,
    JPY,
    KES,
    KGS,
    KHR,
    KMF,
    KPW,
    KRW,
    KWD,
    KYD,
    KZT,
    LAK,
    LBP,
    LKR,
    LRD,
    LSL,
    LYD,
    MAD,
    MDL,
    MGA,
    MKD,
    MMK,
    MNT,
    MOP,
    MRU,
    MUR,
    MVR,
    MWK,
    MXN,
    MYR,
    MZN,
    NAD,
    NGN,
    NIO,
    NOK,
    NPR,
    NZD,
    OMR,
    PAB,
    PEN,
    PGK,
    PHP,
    PKR,
    PLN,
    PYG,
    QAR,
    RON,
    RSD,
    RUB,
    RWF,
    SAR,
    SBD,
    SCR,
    SDG,
    SEK,
    SGD,
    SHP,
    SLL,
    SOS,
    SRD,
    STN,
    SVC,
    SYP,
    SZL,
    THB,
    TJS,
    TMT,
    TND,
    TOP,
    TRY,
    TTD,
    TWD,
    TZS,
    UAH,
    UGX,
    USD,
    UYU,
    UZS,
    VND,
    VUV,
    WST,
    XAF,
    XCD,
    XDR,
    XOF,
    XPF,
    YER,
    ZAR,
    ZMW
  )

  final case object AED extends Currency
  final case object AFN extends Currency
  final case object ALL extends Currency
  final case object AMD extends Currency
  final case object ANG extends Currency
  final case object AOA extends Currency
  final case object ARS extends Currency
  final case object AUD extends Currency
  final case object AWG extends Currency
  final case object AZN extends Currency
  final case object BAM extends Currency
  final case object BBD extends Currency
  final case object BDT extends Currency
  final case object BGN extends Currency
  final case object BHD extends Currency
  final case object BIF extends Currency
  final case object BMD extends Currency
  final case object BND extends Currency
  final case object BOB extends Currency
  final case object BRL extends Currency
  final case object BSD extends Currency
  final case object BTN extends Currency
  final case object BWP extends Currency
  final case object BYN extends Currency
  final case object BZD extends Currency
  final case object CAD extends Currency
  final case object CDF extends Currency
  final case object CHF extends Currency
  final case object CLP extends Currency
  final case object CNY extends Currency
  final case object COP extends Currency
  final case object CRC extends Currency
  final case object CUC extends Currency
  final case object CUP extends Currency
  final case object CVE extends Currency
  final case object CZK extends Currency
  final case object DJF extends Currency
  final case object DKK extends Currency
  final case object DOP extends Currency
  final case object DZD extends Currency
  final case object EGP extends Currency
  final case object ERN extends Currency
  final case object ETB extends Currency
  final case object EUR extends Currency
  final case object FJD extends Currency
  final case object FKP extends Currency
  final case object GBP extends Currency
  final case object GEL extends Currency
  final case object GHS extends Currency
  final case object GIP extends Currency
  final case object GMD extends Currency
  final case object GNF extends Currency
  final case object GTQ extends Currency
  final case object GYD extends Currency
  final case object HKD extends Currency
  final case object HNL extends Currency
  final case object HTG extends Currency
  final case object HUF extends Currency
  final case object IDR extends Currency
  final case object ILS extends Currency
  final case object INR extends Currency
  final case object IQD extends Currency
  final case object IRR extends Currency
  final case object ISK extends Currency
  final case object JMD extends Currency
  final case object JOD extends Currency
  final case object JPY extends Currency
  final case object KES extends Currency
  final case object KGS extends Currency
  final case object KHR extends Currency
  final case object KMF extends Currency
  final case object KPW extends Currency
  final case object KRW extends Currency
  final case object KWD extends Currency
  final case object KYD extends Currency
  final case object KZT extends Currency
  final case object LAK extends Currency
  final case object LBP extends Currency
  final case object LKR extends Currency
  final case object LRD extends Currency
  final case object LSL extends Currency
  final case object LYD extends Currency
  final case object MAD extends Currency
  final case object MDL extends Currency
  final case object MGA extends Currency
  final case object MKD extends Currency
  final case object MMK extends Currency
  final case object MNT extends Currency
  final case object MOP extends Currency
  final case object MRU extends Currency
  final case object MUR extends Currency
  final case object MVR extends Currency
  final case object MWK extends Currency
  final case object MXN extends Currency
  final case object MYR extends Currency
  final case object MZN extends Currency
  final case object NAD extends Currency
  final case object NGN extends Currency
  final case object NIO extends Currency
  final case object NOK extends Currency
  final case object NPR extends Currency
  final case object NZD extends Currency
  final case object OMR extends Currency
  final case object PAB extends Currency
  final case object PEN extends Currency
  final case object PGK extends Currency
  final case object PHP extends Currency
  final case object PKR extends Currency
  final case object PLN extends Currency
  final case object PYG extends Currency
  final case object QAR extends Currency
  final case object RON extends Currency
  final case object RSD extends Currency
  final case object RUB extends Currency
  final case object RWF extends Currency
  final case object SAR extends Currency
  final case object SBD extends Currency
  final case object SCR extends Currency
  final case object SDG extends Currency
  final case object SEK extends Currency
  final case object SGD extends Currency
  final case object SHP extends Currency
  final case object SLL extends Currency
  final case object SOS extends Currency
  final case object SRD extends Currency
  final case object STN extends Currency
  final case object SVC extends Currency
  final case object SYP extends Currency
  final case object SZL extends Currency
  final case object THB extends Currency
  final case object TJS extends Currency
  final case object TMT extends Currency
  final case object TND extends Currency
  final case object TOP extends Currency
  final case object TRY extends Currency
  final case object TTD extends Currency
  final case object TWD extends Currency
  final case object TZS extends Currency
  final case object UAH extends Currency
  final case object UGX extends Currency
  final case object USD extends Currency
  final case object UYU extends Currency
  final case object UZS extends Currency
  final case object VND extends Currency
  final case object VUV extends Currency
  final case object WST extends Currency
  final case object XAF extends Currency
  final case object XCD extends Currency
  final case object XDR extends Currency
  final case object XOF extends Currency
  final case object XPF extends Currency
  final case object YER extends Currency
  final case object ZAR extends Currency
  final case object ZMW extends Currency

  def fromString(code: String): Either[InvalidCurrencyCode, Currency] =
    code.toUpperCase match {
      case "AED" => Right(AED)
      case "AFN" => Right(AFN)
      case "ALL" => Right(ALL)
      case "AMD" => Right(AMD)
      case "ANG" => Right(ANG)
      case "AOA" => Right(AOA)
      case "ARS" => Right(ARS)
      case "AUD" => Right(AUD)
      case "AWG" => Right(AWG)
      case "AZN" => Right(AZN)
      case "BAM" => Right(BAM)
      case "BBD" => Right(BBD)
      case "BDT" => Right(BDT)
      case "BGN" => Right(BGN)
      case "BHD" => Right(BHD)
      case "BIF" => Right(BIF)
      case "BMD" => Right(BMD)
      case "BND" => Right(BND)
      case "BOB" => Right(BOB)
      case "BRL" => Right(BRL)
      case "BSD" => Right(BSD)
      case "BTN" => Right(BTN)
      case "BWP" => Right(BWP)
      case "BYN" => Right(BYN)
      case "BZD" => Right(BZD)
      case "CAD" => Right(CAD)
      case "CDF" => Right(CDF)
      case "CHF" => Right(CHF)
      case "CLP" => Right(CLP)
      case "CNY" => Right(CNY)
      case "COP" => Right(COP)
      case "CRC" => Right(CRC)
      case "CUC" => Right(CUC)
      case "CUP" => Right(CUP)
      case "CVE" => Right(CVE)
      case "CZK" => Right(CZK)
      case "DJF" => Right(DJF)
      case "DKK" => Right(DKK)
      case "DOP" => Right(DOP)
      case "DZD" => Right(DZD)
      case "EGP" => Right(EGP)
      case "ERN" => Right(ERN)
      case "ETB" => Right(ETB)
      case "EUR" => Right(EUR)
      case "FJD" => Right(FJD)
      case "FKP" => Right(FKP)
      case "GBP" => Right(GBP)
      case "GEL" => Right(GEL)
      case "GHS" => Right(GHS)
      case "GIP" => Right(GIP)
      case "GMD" => Right(GMD)
      case "GNF" => Right(GNF)
      case "GTQ" => Right(GTQ)
      case "GYD" => Right(GYD)
      case "HKD" => Right(HKD)
      case "HNL" => Right(HNL)
      case "HTG" => Right(HTG)
      case "HUF" => Right(HUF)
      case "IDR" => Right(IDR)
      case "ILS" => Right(ILS)
      case "INR" => Right(INR)
      case "IQD" => Right(IQD)
      case "IRR" => Right(IRR)
      case "ISK" => Right(ISK)
      case "JMD" => Right(JMD)
      case "JOD" => Right(JOD)
      case "JPY" => Right(JPY)
      case "KES" => Right(KES)
      case "KGS" => Right(KGS)
      case "KHR" => Right(KHR)
      case "KMF" => Right(KMF)
      case "KPW" => Right(KPW)
      case "KRW" => Right(KRW)
      case "KWD" => Right(KWD)
      case "KYD" => Right(KYD)
      case "KZT" => Right(KZT)
      case "LAK" => Right(LAK)
      case "LBP" => Right(LBP)
      case "LKR" => Right(LKR)
      case "LRD" => Right(LRD)
      case "LSL" => Right(LSL)
      case "LYD" => Right(LYD)
      case "MAD" => Right(MAD)
      case "MDL" => Right(MDL)
      case "MGA" => Right(MGA)
      case "MKD" => Right(MKD)
      case "MMK" => Right(MMK)
      case "MNT" => Right(MNT)
      case "MOP" => Right(MOP)
      case "MRU" => Right(MRU)
      case "MUR" => Right(MUR)
      case "MVR" => Right(MVR)
      case "MWK" => Right(MWK)
      case "MXN" => Right(MXN)
      case "MYR" => Right(MYR)
      case "MZN" => Right(MZN)
      case "NAD" => Right(NAD)
      case "NGN" => Right(NGN)
      case "NIO" => Right(NIO)
      case "NOK" => Right(NOK)
      case "NPR" => Right(NPR)
      case "NZD" => Right(NZD)
      case "OMR" => Right(OMR)
      case "PAB" => Right(PAB)
      case "PEN" => Right(PEN)
      case "PGK" => Right(PGK)
      case "PHP" => Right(PHP)
      case "PKR" => Right(PKR)
      case "PLN" => Right(PLN)
      case "PYG" => Right(PYG)
      case "QAR" => Right(QAR)
      case "RON" => Right(RON)
      case "RSD" => Right(RSD)
      case "RUB" => Right(RUB)
      case "RWF" => Right(RWF)
      case "SAR" => Right(SAR)
      case "SBD" => Right(SBD)
      case "SCR" => Right(SCR)
      case "SDG" => Right(SDG)
      case "SEK" => Right(SEK)
      case "SGD" => Right(SGD)
      case "SHP" => Right(SHP)
      case "SLL" => Right(SLL)
      case "SOS" => Right(SOS)
      case "SRD" => Right(SRD)
      case "STN" => Right(STN)
      case "SVC" => Right(SVC)
      case "SYP" => Right(SYP)
      case "SZL" => Right(SZL)
      case "THB" => Right(THB)
      case "TJS" => Right(TJS)
      case "TMT" => Right(TMT)
      case "TND" => Right(TND)
      case "TOP" => Right(TOP)
      case "TRY" => Right(TRY)
      case "TTD" => Right(TTD)
      case "TWD" => Right(TWD)
      case "TZS" => Right(TZS)
      case "UAH" => Right(UAH)
      case "UGX" => Right(UGX)
      case "USD" => Right(USD)
      case "UYU" => Right(UYU)
      case "UZS" => Right(UZS)
      case "VND" => Right(VND)
      case "VUV" => Right(VUV)
      case "WST" => Right(WST)
      case "XAF" => Right(XAF)
      case "XCD" => Right(XCD)
      case "XDR" => Right(XDR)
      case "XOF" => Right(XOF)
      case "XPF" => Right(XPF)
      case "YER" => Right(YER)
      case "ZAR" => Right(ZAR)
      case "ZMW" => Right(ZMW)
      case _     => Left(code)
    }

}
