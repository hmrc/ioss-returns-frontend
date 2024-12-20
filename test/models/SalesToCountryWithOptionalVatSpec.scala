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

package models

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsNull, JsObject, Json}

import java.time.LocalDate
import scala.util.Try

class SalesToCountryWithOptionalVatSpec extends SpecBase with Matchers {

  "SalesToCountryWithOptionalVat" - {

    "serialize to JSON correctly" in {

      val vatRateFromCountry = VatRateFromCountry(
        rate = BigDecimal(20),
        rateType = VatRateType.Standard,
        validFrom = LocalDate.of(2020, 1, 1),
        validUntil = Some(LocalDate.of(2025, 1, 1))
      )

      val salesToCountryWithOptionalVat = SalesToCountryWithOptionalVat(
        country = Country("DE", "Germany"),
        vatRatesFromCountry = Some(List(vatRateFromCountry))
      )

      val json = Json.toJson(salesToCountryWithOptionalVat)

      (json \ "country" \ "code").as[String] mustBe "DE"
      (json \ "country" \ "name").as[String] mustBe "Germany"
      (json \ "vatRatesFromCountry").as[List[JsObject]].size mustEqual 1
    }

    "deserialize from JSON correctly" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "vatRatesFromCountry" -> Json.arr(
          Json.obj(
            "rate" -> "20",
            "rateType" -> "STANDARD",
            "validFrom" -> "2020-01-01",
            "validUntil" -> "2025-01-01"
          )
        )
      )

      val result = json.as[SalesToCountryWithOptionalVat]

      result.country.code mustBe "DE"
      result.country.name mustBe "Germany"
      result.vatRatesFromCountry.isDefined mustBe true
      result.vatRatesFromCountry.get.size mustBe 1
      result.vatRatesFromCountry.get.head.rate mustBe BigDecimal(20)
    }

    "handle invalid JSON gracefully during deserialization" in {
      val invalidJson = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE"
        ),
        "vatRatesFromCountry" -> Json.arr(
          Json.obj(
            "rate" -> "20",
            "rateType" -> "STANDARD",
            "validFrom" -> "2020-01-01",
            "validUntil" -> "2025-01-01"
          )
        )
      )

      val result = Try(invalidJson.as[SalesToCountryWithOptionalVat])

      result.isFailure mustBe true
    }

    "correctly handle JSON deserialization when vatRatesFromCountry is missing" in {
      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "vatRatesFromCountry" -> JsNull
      )

      val result = json.as[SalesToCountryWithOptionalVat]

      result.country mustBe Country("DE", "Germany")
      result.vatRatesFromCountry mustBe None
    }
  }
}
