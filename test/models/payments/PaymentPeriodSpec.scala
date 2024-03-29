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

package models.payments

import base.SpecBase
import play.api.libs.json.{Json, JsSuccess}

import java.time.{LocalDate, Month}

class PaymentPeriodSpec extends SpecBase {

//  private val paymentPeriod: PaymentPeriod = arbitraryPaymentPeriod.sample.value

  "PaymentPeriod" - {

    "must deserialise/serialise to and from PaymentPeriod" in {

      val json = Json.obj(
        "year" -> 2023,
        "month" -> 11,
        "dueDate"-> "2023-12-31"
      )

      val expectedResult = PaymentPeriod(
        year = 2023,
        month = Month.NOVEMBER,
        dueDate = LocalDate.of(2023, 12, 31)
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[PaymentPeriod] mustBe JsSuccess(expectedResult)
    }
  }

}
