/*
 * Copyright 2026 HM Revenue & Customs
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

package models

import base.SpecBase
import models.VatRateType.Standard
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}


class EuVatRateSpec extends SpecBase with Matchers {

  "EuVatRate" - {

    "serialize to JSON correctly" in {

      val euVatRate = EuVatRate(
        country = Country("DE", "Germany"),
        vatRate = BigDecimal(0),
        vatRateType = Standard,
        situatedOn = period.firstDay.minusDays(1)
      )

      val expectedJson = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "vatRate" -> 0,
        "vatRateType" -> "STANDARD",
        "situatedOn" -> "2024-02-29"
      )

      Json.toJson(euVatRate) mustBe expectedJson
    }

    "deserialize from JSON correctly" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "vatRate" -> 0,
        "vatRateType" -> "STANDARD",
        "situatedOn" -> "2024-02-29"
      )

      val euVatRate = EuVatRate(
        country = Country("DE", "Germany"),
        vatRate = BigDecimal(0),
        vatRateType = Standard,
        situatedOn = period.firstDay.minusDays(1)
      )

      json.validate[EuVatRate] mustBe JsSuccess(euVatRate)

    }

    "handle invalid JSON gracefully during deserialization" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> 12345
        ),
        "vatRate" -> 0,
        "vatRateType" -> "STANDARD",
        "situatedOn" -> "2024-02-29"
      )

      json.validate[EuVatRate] mustBe a[JsError]
    }

    "correctly handle JSON deserialization when vatRatesFromCountry is missing" in {
      val json = Json.obj()

      json.validate[EuVatRate] mustBe a[JsError]
    }
  }
}
