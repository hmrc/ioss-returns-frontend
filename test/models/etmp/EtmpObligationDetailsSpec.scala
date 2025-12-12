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

class EtmpObligationDetailsSpec extends SpecBase {

  private val etmpObligationDetails: EtmpObligationDetails = arbitraryObligationDetails.arbitrary.sample.value

  "EtmpObligationDetails" - {

    "must deserialise/serialise to and from EtmpObligationDetails" in {

      val json = Json.obj(
        "status" -> etmpObligationDetails.status,
        "periodKey" -> etmpObligationDetails.periodKey
      )

      val expectedResult = EtmpObligationDetails(
        status = etmpObligationDetails.status,
        periodKey = etmpObligationDetails.periodKey
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligationDetails] mustBe JsSuccess(expectedResult)
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "status" -> 12345,
        "periodKey" -> etmpObligationDetails.periodKey
      )

      json.validate[EtmpObligationDetails] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpObligationDetails] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "status" -> JsNull,
        "periodKey" -> etmpObligationDetails.periodKey
      )

      json.validate[EtmpObligationDetails] mustBe a[JsError]
    }
  }
}
