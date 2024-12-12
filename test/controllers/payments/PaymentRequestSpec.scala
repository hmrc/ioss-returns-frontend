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

package controllers.payments

import base.SpecBase
import models.payments.{PaymentPeriod, PaymentRequest}
import play.api.libs.json.Json

import java.time.{LocalDate, Month}

class PaymentRequestSpec extends SpecBase {

  "PaymentPeriod" - {

    "serialize and deserialize correctly" in {
      val paymentPeriod = PaymentPeriod(2023, Month.JUNE, LocalDate.of(2023, 6, 30))
      val paymentRequest = PaymentRequest(
        ioss = "test-ioss",
        period = paymentPeriod,
        amountInPence = 12345,
        dueDate = Some(LocalDate.of(2023, 7, 31))
      )

      val json = Json.toJson(paymentRequest)

      val expectedJson = Json.parse(
        s"""
            {
              "ioss": "test-ioss",
              "period": {
                "year": 2023,
                "month": 6,
                "dueDate": "2023-06-30"
              },
              "amountInPence": 12345,
              "dueDate": "2023-07-31"
            }
            """
      )

      json mustBe expectedJson

      val deserialized = json.as[PaymentRequest]
      deserialized mustBe paymentRequest
    }

    "handle missing optional fields during deserialization" in {
      val json = Json.parse(
        s"""
            {
              "ioss": "test-ioss",
              "period": {
                "year": 2023,
                "month": 6,
                "dueDate": "2023-06-30"
              },
              "amountInPence": 12345
            }
            """
      )

      val expectedPaymentRequest = PaymentRequest(
        ioss = "test-ioss",
        period = PaymentPeriod(2023, Month.JUNE, LocalDate.of(2023, 6, 30)),
        amountInPence = 12345,
        dueDate = None
      )

      val deserialized = json.as[PaymentRequest]
      deserialized mustBe expectedPaymentRequest
    }
  }
}
