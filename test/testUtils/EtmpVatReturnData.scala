/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package testUtils

import base.SpecBase
import models.etmp._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.{LocalDate, LocalDateTime}

object EtmpVatReturnData extends SpecBase {

  private val amountOfGoodsSupplied: Int = Gen.oneOf(List(1, 2, 3)).sample.value
  private val amountOfCorrections: Int = Gen.oneOf(List(1, 2, 3)).sample.value
  private val amountOfBalanceOfVATDueForMS: Int = Gen.oneOf(List(1, 2, 3)).sample.value

  val etmpVatReturnGoodsSupplied: EtmpVatReturnGoodsSupplied = EtmpVatReturnGoodsSupplied(
    msOfConsumption = arbitraryCountry.arbitrary.sample.value.code,
    vatRateType = Gen.oneOf(EtmpVatRateType.values).sample.value,
    taxableAmountGBP = arbitrary[BigDecimal].sample.value,
    vatAmountGBP = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturnCorrection: EtmpVatReturnCorrection = EtmpVatReturnCorrection(
    periodKey = arbitraryPeriodKey.arbitrary.sample.value,
    periodFrom = arbitrary[String].sample.value,
    periodTo = arbitrary[String].sample.value,
    msOfConsumption = arbitraryCountry.arbitrary.sample.value.code,
    totalVATAmountCorrectionGBP = arbitrary[BigDecimal].sample.value,
    totalVATAmountCorrectionEUR = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturnBalanceOfVatDue: EtmpVatReturnBalanceOfVatDue = EtmpVatReturnBalanceOfVatDue(
    msOfConsumption = arbitraryCountry.arbitrary.sample.value.code,
    totalVATDueGBP = arbitrary[BigDecimal].sample.value,
    totalVATEUR = arbitrary[BigDecimal].sample.value
  )

  val etmpVatReturn: EtmpVatReturn = EtmpVatReturn(
    returnReference = arbitrary[String].sample.value,
    returnVersion = arbitrary[LocalDateTime].sample.value,
    periodKey = arbitraryPeriodKey.arbitrary.sample.value,
    returnPeriodFrom = arbitrary[LocalDate].sample.value,
    returnPeriodTo = arbitrary[LocalDate].sample.value,
    goodsSupplied = Gen.listOfN(amountOfGoodsSupplied, etmpVatReturnGoodsSupplied).sample.value,
    totalVATGoodsSuppliedGBP = arbitrary[BigDecimal].sample.value,
    totalVATAmountPayable = arbitrary[BigDecimal].sample.value,
    totalVATAmountPayableAllSpplied = arbitrary[BigDecimal].sample.value,
    correctionPreviousVATReturn = Gen.listOfN(amountOfCorrections, etmpVatReturnCorrection).sample.value,
    totalVATAmountFromCorrectionGBP = arbitrary[BigDecimal].sample.value,
    balanceOfVATDueForMS = Gen.listOfN(amountOfBalanceOfVATDueForMS, etmpVatReturnBalanceOfVatDue).sample.value,
    totalVATAmountDueForAllMSGBP = arbitrary[BigDecimal].sample.value,
    paymentReference = arbitrary[String].sample.value
  )
}
