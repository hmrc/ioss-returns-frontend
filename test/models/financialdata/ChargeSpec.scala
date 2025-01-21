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

package models.financialdata

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class ChargeSpec extends SpecBase {

  private val charge: Charge = arbitraryCharge.arbitrary.sample.value

  "Charge" - {

    "must deserialise/serialise to and from Charge" in {

      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> charge.originalAmount,
        "outstandingAmount" -> charge.outstandingAmount,
        "clearedAmount" -> charge.clearedAmount
      )

      val expectedResult = Charge(
        period = charge.period,
        originalAmount = charge.originalAmount,
        outstandingAmount = charge.outstandingAmount,
        clearedAmount = charge.clearedAmount
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[Charge] mustBe JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[Charge] mustBe a[JsError]
    }

    "must handle missing 'originalAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "outstandingAmount" -> charge.outstandingAmount,
        "clearedAmount" -> charge.clearedAmount
      )

      json.validate[Charge] mustBe a[JsError]
    }

    "must handle missing 'outstandingAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> charge.originalAmount,
        "clearedAmount" -> charge.clearedAmount
      )

      json.validate[Charge] mustBe a[JsError]
    }

    "must handle missing 'clearedAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> charge.originalAmount,
        "outstandingAmount" -> charge.outstandingAmount
      )

      json.validate[Charge] mustBe a[JsError]
    }


    "must handle invalid data in 'originalAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> "not-a-number",
        "outstandingAmount" -> charge.outstandingAmount,
        "clearedAmount" -> charge.clearedAmount
      )

      json.validate[Charge] mustBe a[JsError]
    }

    "must handle invalid data in 'outstandingAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> charge.originalAmount,
        "outstandingAmount" -> "not-a-number",
        "clearedAmount" -> charge.clearedAmount
      )

      json.validate[Charge] mustBe a[JsError]
    }

    "must handle invalid data in 'clearedAmount' field during deserialization" in {
      val json = Json.obj(
        "period" -> charge.period,
        "originalAmount" -> charge.originalAmount,
        "outstandingAmount" -> charge.outstandingAmount,
        "clearedAmount" -> "not-a-number"
      )

      json.validate[Charge] mustBe a[JsError]
    }

    "must serialize and deserialize correctly when amounts are zero" in {
      val chargeWithZeroAmounts = Charge(charge.period, BigDecimal(0), BigDecimal(0), BigDecimal(0))

      val json = Json.obj(
        "period" -> chargeWithZeroAmounts.period,
        "originalAmount" -> chargeWithZeroAmounts.originalAmount,
        "outstandingAmount" -> chargeWithZeroAmounts.outstandingAmount,
        "clearedAmount" -> chargeWithZeroAmounts.clearedAmount
      )

      val expectedResult = chargeWithZeroAmounts

      json mustBe Json.toJson(expectedResult)
      json.validate[Charge] mustBe JsSuccess(expectedResult)
    }

    "must serialize and deserialize correctly with very large amounts" in {
      val largeAmount = BigDecimal("1000000000000000000000000")
      val chargeWithLargeAmount = Charge(charge.period, largeAmount, largeAmount, largeAmount)

      val json = Json.obj(
        "period" -> chargeWithLargeAmount.period,
        "originalAmount" -> chargeWithLargeAmount.originalAmount,
        "outstandingAmount" -> chargeWithLargeAmount.outstandingAmount,
        "clearedAmount" -> chargeWithLargeAmount.clearedAmount
      )

      val expectedResult = chargeWithLargeAmount

      json mustBe Json.toJson(expectedResult)
      json.validate[Charge] mustBe JsSuccess(expectedResult)
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "period" -> JsNull,
        "originalAmount" -> JsNull,
        "outstandingAmount" -> JsNull,
        "clearedAmount" -> JsNull
      )

      json.validate[Charge] mustBe a[JsError]
    }
  }
}
