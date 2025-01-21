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
import testUtils.EtmpVatReturnData.etmpVatReturnBalanceOfVatDue

class EtmpVatReturnBalanceOfVatDueSpec extends SpecBase {

  private val genEtmpVatReturnBalanceOfVatDue: EtmpVatReturnBalanceOfVatDue = etmpVatReturnBalanceOfVatDue

  "EtmpVatReturnBalanceOfVatDue" - {

    "must serialise/deserialise to and from EtmpVatReturnBalanceOfVatDue" in {

      val json = Json.obj(
        "msOfConsumption" -> genEtmpVatReturnBalanceOfVatDue.msOfConsumption,
        "totalVATDueGBP" -> genEtmpVatReturnBalanceOfVatDue.totalVATDueGBP,
        "totalVATEUR" -> genEtmpVatReturnBalanceOfVatDue.totalVATEUR
      )

      val expectedResult = EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = genEtmpVatReturnBalanceOfVatDue.msOfConsumption,
        totalVATDueGBP = genEtmpVatReturnBalanceOfVatDue.totalVATDueGBP,
        totalVATEUR = genEtmpVatReturnBalanceOfVatDue.totalVATEUR
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpVatReturnBalanceOfVatDue] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[EtmpVatReturnBalanceOfVatDue] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "msOfConsumption" -> 12345,
        "totalVATDueGBP" -> genEtmpVatReturnBalanceOfVatDue.totalVATDueGBP,
        "totalVATEUR" -> genEtmpVatReturnBalanceOfVatDue.totalVATEUR
      )

      json.validate[EtmpVatReturnBalanceOfVatDue] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {
      val json = Json.obj(
        "msOfConsumption" -> JsNull,
        "totalVATDueGBP" -> genEtmpVatReturnBalanceOfVatDue.totalVATDueGBP,
        "totalVATEUR" -> genEtmpVatReturnBalanceOfVatDue.totalVATEUR
      )

      json.validate[EtmpVatReturnBalanceOfVatDue] mustBe a[JsError]
    }
  }
}
