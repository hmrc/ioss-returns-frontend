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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

import java.time.LocalDate

class EtmpExclusionSpec extends SpecBase {

  private val exclusionReason: EtmpExclusionReason = arbitraryEtmpExclusionReason.arbitrary.sample.value
  private val etmpExclusion: EtmpExclusion = arbitraryEtmpExclusion.arbitrary.sample.value
  private val effectiveDate: LocalDate = etmpExclusion.effectiveDate
  private val decisionDate: LocalDate = etmpExclusion.decisionDate
  private val quarantine: Boolean = etmpExclusion.quarantine

  "EtmpExclusion" - {

    "must serialise/deserialise to and from EtmpExclusion" in {

      val etmpExclusion: EtmpExclusion = EtmpExclusion(
        exclusionReason = exclusionReason,
        effectiveDate = effectiveDate,
        decisionDate = decisionDate,
        quarantine = quarantine
      )

      val expectedJson = Json.obj(
        "exclusionReason" -> exclusionReason,
        "effectiveDate" -> effectiveDate,
        "decisionDate" -> decisionDate,
        "quarantine" -> quarantine
      )

      Json.toJson(etmpExclusion) mustBe expectedJson
      expectedJson.validate[EtmpExclusion] mustBe JsSuccess(etmpExclusion)
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[EtmpExclusion] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "exclusionReason" -> 12345,
        "effectiveDate" -> effectiveDate,
        "decisionDate" -> decisionDate,
        "quarantine" -> quarantine
      )

      expectedJson.validate[EtmpExclusion] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val expectedJson = Json.obj(
        "exclusionReason" -> JsNull,
        "effectiveDate" -> effectiveDate,
        "decisionDate" -> decisionDate,
        "quarantine" -> quarantine
      )

      expectedJson.validate[EtmpExclusion] mustBe a[JsError]
    }
  }
}
