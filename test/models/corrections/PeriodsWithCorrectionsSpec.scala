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

package models.corrections

import base.SpecBase
import models.{Country, StandardPeriod}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

import java.time.Month

class PeriodsWithCorrectionsSpec extends AnyFreeSpec with Matchers with SpecBase {

  val correctionToCountry: CorrectionToCountry = CorrectionToCountry(Country("DE", "Germany"), Some(BigDecimal(100)))
  val standardPeriod: StandardPeriod = StandardPeriod(2023, Month.JANUARY)

  val periodWithCorrections: PeriodWithCorrections = PeriodWithCorrections(
    correctionReturnPeriod = standardPeriod,
    correctionsToCountry = Some(List(correctionToCountry))
  )

  "PeriodsWithCorrections" - {

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(periodWithCorrections)
      val deserializedObject = serializedJson.as[PeriodWithCorrections]

      deserializedObject mustEqual periodWithCorrections
    }

    "handle optional countryVatCorrection (None value)" in {
      val correctionToCountryWithoutVat = correctionToCountry.copy(countryVatCorrection = None)
      val serializedJsonWithoutVat = Json.toJson(correctionToCountryWithoutVat)

      (serializedJsonWithoutVat \ "countryVatCorrection").asOpt[BigDecimal] mustBe None
    }

    "handle missing fields in JSON" in {
      val invalidJson = Json.obj()

      val result = invalidJson.validate[CorrectionToCountry]
      result.isError mustBe true
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "correctionCountry" -> Json.obj(
          "code" -> "DE",
          "name" -> "Germany"
        ),
        "countryVatCorrection" -> "invalid"
      )

      val result = invalidJson.validate[CorrectionToCountry]
      result.isError mustBe true
    }
  }

}
