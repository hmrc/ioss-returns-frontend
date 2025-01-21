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

package models.enrolements

import base.SpecBase
import models.enrolments.EACDIdentifiers
import play.api.libs.json.{JsError, JsSuccess, Json}

class EACDIdentifiersSpec extends SpecBase {

  "EACDIdentifiers" - {

    "must serialize to JSON correctly" in {
      
      val eACDIdentifiers = EACDIdentifiers("IossNumber", "IM9009876543")

      val expectedJson = Json.obj(
        "key" -> "IossNumber",
        "value" -> "IM9009876543"
      )

      Json.toJson(eACDIdentifiers) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "key" -> "IossNumber",
        "value" -> "IM9009876543"
      )

      val eACDIdentifiers = EACDIdentifiers("IossNumber", "IM9009876543")

      json.validate[EACDIdentifiers] mustBe JsSuccess(eACDIdentifiers)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EACDIdentifiers] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "key" -> "IossNumber",
        "value" -> 1234566789
      )

      json.validate[EACDIdentifiers] mustBe a[JsError]
    }
  }
}
