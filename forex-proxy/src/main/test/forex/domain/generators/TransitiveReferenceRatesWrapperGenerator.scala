package forex.domain.generators

import forex.domain.logic.TransitiveReferenceRatesWrapper
import forex.domain.logic.TransitiveReferenceRatesWrapper._
import org.scalacheck.Gen

object TransitiveReferenceRatesWrapperGenerator {
  def genOppositeToRefRate[AsRate](genReferenceValue: Gen[AsRate]): Gen[OppositeToRefRate[AsRate]] =
    genReferenceValue.map(OppositeToRefRate.apply)

  def genReferenceRateWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[ReferenceRate[AsRate]] =
    genReferenceValue.map(ReferenceRate.apply)

  def genTransitiveRefRatesCoupleWrapper[AsRate](
      genReferenceValue: Gen[AsRate]
  ): Gen[TransitiveRefRatesCouple[AsRate]] =
    for {
      fromRef <- genReferenceValue
      toRef <- genReferenceValue
    } yield TransitiveRefRatesCouple(fromRef, toRef)

  def genTransitiveReferenceRatesWrapper[AsRate](genReferenceValue: Gen[AsRate]): Gen[TransitiveReferenceRatesWrapper[AsRate]] =
    Gen.oneOf(
      genReferenceRateWrapper(genReferenceValue),
      genOppositeToRefRate(genReferenceValue),
      genTransitiveRefRatesCoupleWrapper(genReferenceValue)
    )

}
