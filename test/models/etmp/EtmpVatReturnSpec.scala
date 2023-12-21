/*
 * Copyright 2023 HM Revenue & Customs
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

package models.etmp
import models.etmp.EtmpVatReturn._
import base.SpecBase

import java.time.LocalDate

class EtmpVatReturnSpec extends SpecBase {
  val goodSupplied1 = EtmpVatReturnGoodsSupplied("IE", EtmpVatRateType.ReducedVatRate, BigDecimal(100), BigDecimal(15))
  val goodSupplied2 = EtmpVatReturnGoodsSupplied("IT", EtmpVatRateType.StandardVatRate, BigDecimal(200), BigDecimal(20))

  val etmpVatReturnWithoutCorrection = EtmpVatReturn("", "", LocalDate.now(), LocalDate.now(), List(goodSupplied1, goodSupplied2), BigDecimal(0), BigDecimal(0),
    BigDecimal(0), Nil, BigDecimal(0), Nil, BigDecimal(0), "")

  val etmpVatReturnWithCorrection = etmpVatReturnWithoutCorrection.copy(totalVATAmountFromCorrectionGBP = BigDecimal(2.5))

  "EtmpVatReturn" - {
    "should getTotalVatOnSalesAfterCorrection correctly when there is no correction" in {
      etmpVatReturnWithoutCorrection.getTotalVatOnSalesAfterCorrection() mustBe BigDecimal(35)
    }
    "should getTotalVatOnSalesAfterCorrection correctly when there is correction" in {
      etmpVatReturnWithCorrection.getTotalVatOnSalesAfterCorrection() mustBe BigDecimal(37.5)
    }
  }
}
