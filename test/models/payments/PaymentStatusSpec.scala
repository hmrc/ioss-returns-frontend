/*
 * Copyright 2025 HM Revenue & Customs
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
import models.payments.PaymentStatus._
import play.api.libs.json._

class PaymentStatusSpec extends SpecBase {

  "PaymentStatus.reads" - {

    "deserialize 'UNPAID' to Unpaid" in {
      val json = Json.parse("\"UNPAID\"")
      json.as[PaymentStatus] mustBe PaymentStatus.Unpaid
    }

    "deserialize 'PARTIAL' to Partial" in {
      val json = Json.parse("\"PARTIAL\"")
      json.as[PaymentStatus] mustBe PaymentStatus.Partial
    }

    "deserialize 'PAID' to Paid" in {
      val json = Json.parse("\"PAID\"")
      json.as[PaymentStatus] mustBe PaymentStatus.Paid
    }

    "deserialize 'UNKNOWN' to Unknown" in {
      val json = Json.parse("\"UNKNOWN\"")
      json.as[PaymentStatus] mustBe PaymentStatus.Unknown
    }

    "deserialize 'NIL_RETURN' to NilReturn" in {
      val json = Json.parse("\"NIL_RETURN\"")
      json.as[PaymentStatus] mustBe PaymentStatus.NilReturn
    }

    "deserialize 'EXCLUDED' to Excluded" in {
      val json = Json.parse("\"EXCLUDED\"")
      json.as[PaymentStatus] mustBe PaymentStatus.Excluded
    }

  }

  "PaymentStatus.writes" - {

    "serialize Unpaid to 'UNPAID'" in {
      val paymentStatus = PaymentStatus.Unpaid

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("UNPAID")
    }

    "serialize Partial to 'PARTIAL'" in {
      val paymentStatus = PaymentStatus.Partial

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("PARTIAL")
    }

    "serialize Paid to 'PAID'" in {
      val paymentStatus = PaymentStatus.Paid

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("PAID")
    }

    "serialize Unknown to 'UNKNOWN'" in {
      val paymentStatus = PaymentStatus.Unknown

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("UNKNOWN")
    }

    "serialize NilReturn to 'NIL_RETURN'" in {
      val paymentStatus = PaymentStatus.NilReturn

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("NIL_RETURN")
    }

    "serialize Excluded to 'EXCLUDED'" in {
      val paymentStatus = PaymentStatus.Excluded

      val json = Json.toJson[PaymentStatus](paymentStatus)

      json mustBe JsString("EXCLUDED")
    }
  }

  "PaymentStatus.values" - {

    "contain all the possible PaymentStatus values" in {
      PaymentStatus.values must contain theSameElementsAs Seq(
        PaymentStatus.Unpaid, PaymentStatus.Partial, PaymentStatus.Paid,
        PaymentStatus.Unknown, PaymentStatus.NilReturn, PaymentStatus.Excluded
      )
    }
  }
}