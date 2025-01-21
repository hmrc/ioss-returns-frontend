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

package models.requests

import base.SpecBase
import models.UserAnswers
import play.api.libs.json.{JsError, JsNull, Json}

class SaveForLaterRequestSpec extends SpecBase {

  "SaveForLaterRequest" - {

    "serialize and deserialize correctly" in {

      val data = Json.obj("key" -> "value")
      val saveForLaterRequest = SaveForLaterRequest(
        iossNumber = iossNumber,
        period = period,
        data = data
      )

      val json = Json.toJson(saveForLaterRequest)

      val expectedJson = Json.parse(
        s"""
        {
          "iossNumber": "IM9001234567",
          "period": {
            "year": 2024,
            "month": "M3"
          },
          "data": {
            "key": "value"
          }
        }
        """
      )

      json mustBe expectedJson

      val deserialized = json.as[SaveForLaterRequest]
      deserialized mustBe saveForLaterRequest
    }

    "handle UserAnswers correctly in the apply method" in {

      val answers = UserAnswers(id = "some-id", period = period, data = Json.obj("anotherKey" -> "anotherValue"))
      val iossNumber = "IM9001234569"

      val saveForLaterRequest = SaveForLaterRequest(answers, iossNumber, period)

      val expectedSaveForLaterRequest = SaveForLaterRequest(
        iossNumber = "IM9001234569",
        period = period,
        data = Json.obj("anotherKey" -> "anotherValue")
      )

      saveForLaterRequest mustBe expectedSaveForLaterRequest
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[SaveForLaterRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val expectedJson = Json.obj(
        "iossNumber" -> 1234567,
        "period" -> Json.obj(
          "year" -> 2024,
          "month" -> "M3"
        ),
        "data" -> Json.obj(
          "key" -> "value"
        )
      )

      expectedJson.validate[SaveForLaterRequest] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {
      val expectedJson = Json.obj(
        "iossNumber" -> JsNull,
        "period" -> JsNull,
        "data" -> JsNull
      )

      expectedJson.validate[SaveForLaterRequest] mustBe a[JsError]
    }
  }
}

