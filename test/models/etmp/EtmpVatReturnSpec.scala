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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}
import testUtils.EtmpVatReturnData.etmpVatReturn

class EtmpVatReturnSpec extends SpecBase {

  private val genEtmpVatReturn: EtmpVatReturn = etmpVatReturn

  "EtmpVatReturn" - {

    "must serialise/deserialise to and from EtmpVatReturn" in {

      val json = Json.obj(
        "returnReference" -> genEtmpVatReturn.returnReference,
        "returnVersion" -> genEtmpVatReturn.returnVersion,
        "periodKey" -> genEtmpVatReturn.periodKey,
        "returnPeriodFrom" -> genEtmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> genEtmpVatReturn.returnPeriodTo,
        "goodsSupplied" -> genEtmpVatReturn.goodsSupplied,
        "totalVATGoodsSuppliedGBP" -> genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        "totalVATAmountPayable" -> genEtmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        "correctionPreviousVATReturn" -> genEtmpVatReturn.correctionPreviousVATReturn,
        "totalVATAmountFromCorrectionGBP" -> genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        "balanceOfVATDueForMS" -> genEtmpVatReturn.balanceOfVATDueForMS,
        "totalVATAmountDueForAllMSGBP" -> genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> genEtmpVatReturn.paymentReference
      )

      val expectedResult = EtmpVatReturn(
        returnReference = genEtmpVatReturn.returnReference,
        returnVersion = genEtmpVatReturn.returnVersion,
        periodKey = genEtmpVatReturn.periodKey,
        returnPeriodFrom = genEtmpVatReturn.returnPeriodFrom,
        returnPeriodTo = genEtmpVatReturn.returnPeriodTo,
        goodsSupplied = genEtmpVatReturn.goodsSupplied,
        totalVATGoodsSuppliedGBP = genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = genEtmpVatReturn.totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = genEtmpVatReturn.correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = genEtmpVatReturn.balanceOfVATDueForMS,
        totalVATAmountDueForAllMSGBP = genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        paymentReference = genEtmpVatReturn.paymentReference
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturn] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpVatReturn] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "returnReference" -> 12345,
        "returnVersion" -> genEtmpVatReturn.returnVersion,
        "periodKey" -> genEtmpVatReturn.periodKey,
        "returnPeriodFrom" -> genEtmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> genEtmpVatReturn.returnPeriodTo,
        "goodsSupplied" -> genEtmpVatReturn.goodsSupplied,
        "totalVATGoodsSuppliedGBP" -> genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        "totalVATAmountPayable" -> genEtmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        "correctionPreviousVATReturn" -> genEtmpVatReturn.correctionPreviousVATReturn,
        "totalVATAmountFromCorrectionGBP" -> genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        "balanceOfVATDueForMS" -> genEtmpVatReturn.balanceOfVATDueForMS,
        "totalVATAmountDueForAllMSGBP" -> genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> genEtmpVatReturn.paymentReference
      )

      json.validate[EtmpVatReturn] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "returnReference" -> JsNull,
        "returnVersion" -> genEtmpVatReturn.returnVersion,
        "periodKey" -> genEtmpVatReturn.periodKey,
        "returnPeriodFrom" -> genEtmpVatReturn.returnPeriodFrom,
        "returnPeriodTo" -> genEtmpVatReturn.returnPeriodTo,
        "goodsSupplied" -> genEtmpVatReturn.goodsSupplied,
        "totalVATGoodsSuppliedGBP" -> genEtmpVatReturn.totalVATGoodsSuppliedGBP,
        "totalVATAmountPayable" -> genEtmpVatReturn.totalVATAmountPayable,
        "totalVATAmountPayableAllSpplied" -> genEtmpVatReturn.totalVATAmountPayableAllSpplied,
        "correctionPreviousVATReturn" -> genEtmpVatReturn.correctionPreviousVATReturn,
        "totalVATAmountFromCorrectionGBP" -> genEtmpVatReturn.totalVATAmountFromCorrectionGBP,
        "balanceOfVATDueForMS" -> genEtmpVatReturn.balanceOfVATDueForMS,
        "totalVATAmountDueForAllMSGBP" -> genEtmpVatReturn.totalVATAmountDueForAllMSGBP,
        "paymentReference" -> genEtmpVatReturn.paymentReference
      )

      json.validate[EtmpVatReturn] mustBe a[JsError]
    }
  }
}
