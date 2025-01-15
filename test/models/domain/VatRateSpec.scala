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

package models.domain

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class VatRateSpec extends SpecBase with Matchers {

  "VatRate" - {

    "return rateForDisplay without decimals for whole numbers" in {
      val vatRate = VatRate(BigDecimal(20), VatRateType.Standard)
      vatRate.rateForDisplay mustBe "20%"
    }

    "return rateForDisplay with decimals for fractional rates" in {
      val vatRate = VatRate(BigDecimal(5.5), VatRateType.Reduced)
      vatRate.rateForDisplay mustBe  "5.5%"
    }

    "serialize to JSON correctly" in {
      val vatRate = VatRate(BigDecimal(20), VatRateType.Standard)
      val json = Json.toJson(vatRate)

      (json \ "rate").as[BigDecimal] mustBe 20
      (json \ "rateType").as[String] mustBe "STANDARD"
    }

    "deserialize from JSON correctly" in {
      val json =
        """{
          |  "rate": 5.5,
          |  "rateType": "REDUCED"
          |}""".stripMargin

      val vatRate = Json.parse(json).as[VatRate]
      vatRate.rate mustBe BigDecimal(5.5)
      vatRate.rateType mustBe VatRateType.Reduced
    }
  }
  
}