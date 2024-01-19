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
import play.api.libs.json.{JsSuccess, Json}

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
  }
}
