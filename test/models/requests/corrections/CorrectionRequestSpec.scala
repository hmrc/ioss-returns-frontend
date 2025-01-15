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

package models.requests.corrections

import base.SpecBase
import models.Country
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import play.api.libs.json.{JsNull, Json}

class CorrectionRequestSpec extends SpecBase {

  "CorrectionRequest" - {

    "create a CorrectionRequest with the correct values" in {

      val corrections = List(
        PeriodWithCorrections(
          correctionReturnPeriod = period,
          correctionsToCountry = Some(List(CorrectionToCountry(
            Country("ES", "Spain"),
            Some(BigDecimal(500))
          )))
        )
      )

      val correctionRequest = CorrectionRequest(vrn, period, corrections)

      correctionRequest.vrn mustBe vrn
      correctionRequest.period mustBe period
      correctionRequest.corrections mustBe corrections
    }

    "handle None for correctionsToCountry correctly" in {

      val corrections = List(
        PeriodWithCorrections(
          correctionReturnPeriod = period,
          correctionsToCountry = None
        )
      )

      val correctionRequest = CorrectionRequest(vrn, period, corrections)

      correctionRequest.corrections.head.correctionsToCountry mustBe None
    }
  }

  "serialize and deserialize correctly" in {
    val corrections = List(
      PeriodWithCorrections(
        correctionReturnPeriod = period,
        correctionsToCountry = Some(List(CorrectionToCountry(
          Country("FR", "France"),
          Some(BigDecimal(1000))
        )))
      )
    )

    val correctionRequest = CorrectionRequest(vrn, period, corrections)

    val json = Json.toJson(correctionRequest)
    val deserialized = json.as[CorrectionRequest]

    deserialized mustBe correctionRequest
  }

  "deserialize JSON with an empty corrections list" in {
    val json = Json.obj(
      "vrn" -> vrn.value,
      "period" -> period,
      "corrections" -> Json.arr()
    )

    val result = json.validate[CorrectionRequest]

    result.isSuccess mustBe true
    result.get.corrections mustBe List.empty
  }

  "fail to deserialize JSON with missing required fields" in {
    val invalidJson = Json.obj(
      "period" -> period,
      "corrections" -> Json.arr()
    )

    val result = invalidJson.validate[CorrectionRequest]

    result.isError mustBe true
  }

  "correctly handle a correction with an empty list of correctionsToCountry" in {
    val corrections = List(
      PeriodWithCorrections(
        correctionReturnPeriod = period,
        correctionsToCountry = Some(List.empty)
      )
    )

    val correctionRequest = CorrectionRequest(vrn, period, corrections)

    correctionRequest.corrections.head.correctionsToCountry mustBe Some(List.empty)
  }

  "deserialize JSON with corrections containing None correctionsToCountry" in {
    val json = Json.obj(
      "vrn" -> vrn.value,
      "period" -> period,
      "corrections" -> Json.arr(
        Json.obj(
          "correctionReturnPeriod" -> period,
          "correctionsToCountry" -> JsNull
        )
      )
    )

    val result = json.validate[CorrectionRequest]

    result.isSuccess mustBe true
    result.get.corrections.head.correctionsToCountry mustBe None
  }

  "fail to deserialize JSON with invalid correctionsToCountry format" in {
    val invalidJson = Json.obj(
      "vrn" -> vrn.value,
      "period" -> period,
      "corrections" -> Json.arr(
        Json.obj(
          "correctionReturnPeriod" -> period,
          "correctionsToCountry" -> Json.arr(
            Json.obj(
              "correctionCountry" -> Json.obj(
                "code" -> "DE"
              ),
              "countryVatCorrection" -> "invalid" // Invalid BigDecimal format
            )
          )
        )
      )
    )

    val result = invalidJson.validate[CorrectionRequest]

    result.isError mustBe true
  }
}
