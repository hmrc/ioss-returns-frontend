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

package models

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import scala.util.Try

class TotalVatToCountrySpec extends SpecBase with Matchers {

  "TotalVatToCountry" - {

    "serialize to JSON correctly" in {

      val country = Country("DE", "Germany")
      val totalVat = BigDecimal(1234.56)
      val totalVatToCountry = TotalVatToCountry(country, totalVat)

      val json = Json.toJson(totalVatToCountry)

      (json \ "country" \ "code").as[String] mustBe "DE"
      (json \ "country" \ "name").as[String] mustBe "Germany"
      (json \ "totalVat").as[BigDecimal] mustBe 1234.56
    }

    "deserialize from JSON correctly" in {

      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "totalVat" -> 1234.56
      )

      val result = json.as[TotalVatToCountry]

      result.country.code mustBe "DE"
      result.country.name mustBe "Germany"
      result.totalVat mustBe BigDecimal(1234.56)
    }

    "handle invalid JSON gracefully during deserialization" in {
      val invalidJson = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE"
        ),
        "totalVat" -> 1234.56
      )

      val result = Try(invalidJson.as[TotalVatToCountry])

      result.isFailure mustBe true
    }

    "correctly handle JSON deserialization when totalVat is missing" in {
      val json = Json.obj(
        "country" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        )
      )

      val result = Try(json.as[TotalVatToCountry])

      result.isFailure mustBe true
    }
  }
}
