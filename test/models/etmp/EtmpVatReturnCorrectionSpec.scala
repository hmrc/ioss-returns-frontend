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

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}
import testUtils.EtmpVatReturnData.etmpVatReturnCorrection

class EtmpVatReturnCorrectionSpec extends SpecBase {

  private val genEtmpVatReturnCorrection: EtmpVatReturnCorrection = etmpVatReturnCorrection

  "EtmpVatReturnCorrection" - {

    "must serialise/deserialise to and from EtmpVatReturnCorrection" in {

      val json = Json.obj(
        "periodKey" -> genEtmpVatReturnCorrection.periodKey,
        "periodFrom" -> genEtmpVatReturnCorrection.periodFrom,
        "periodTo" -> genEtmpVatReturnCorrection.periodTo,
        "msOfConsumption" -> genEtmpVatReturnCorrection.msOfConsumption,
        "totalVATAmountCorrectionGBP" -> genEtmpVatReturnCorrection.totalVATAmountCorrectionGBP,
        "totalVATAmountCorrectionEUR" -> genEtmpVatReturnCorrection.totalVATAmountCorrectionEUR
      )

      val expectedResult = EtmpVatReturnCorrection(
        periodKey = genEtmpVatReturnCorrection.periodKey,
        periodFrom = genEtmpVatReturnCorrection.periodFrom,
        periodTo = genEtmpVatReturnCorrection.periodTo,
        msOfConsumption = genEtmpVatReturnCorrection.msOfConsumption,
        totalVATAmountCorrectionGBP = genEtmpVatReturnCorrection.totalVATAmountCorrectionGBP,
        totalVATAmountCorrectionEUR = genEtmpVatReturnCorrection.totalVATAmountCorrectionEUR
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturnCorrection] mustBe JsSuccess(expectedResult)
    }
  }
}
