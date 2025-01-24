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

package models.external

import base.SpecBase
import play.api.libs.json.*


class ExternalEntryUrlSpec extends SpecBase {

  "ExternalEntryUrl" - {

    "serialize correctly to JSON" in {

      val externalEntryUrl = ExternalEntryUrl(Some("/url"))

      val expectedJson = Json.obj(
        "url" -> "/url"
      )

      Json.toJson(externalEntryUrl) mustBe expectedJson
    }

    "deserialize correctly from JSON" in {

      val json = Json.obj(
        "url" -> "/url"
      )

      val externalEntryUrl = ExternalEntryUrl(Some("/url"))

      json.validate[ExternalEntryUrl] mustBe JsSuccess(externalEntryUrl)
    }

    "fail to deserialize if a required field is missing" in {
      val json = Json.obj()

      val excpectedExternalEntryUrl = ExternalEntryUrl(None)

      json.validate[ExternalEntryUrl] mustBe JsSuccess(excpectedExternalEntryUrl)
    }

    "serialize correctly to JSON when url is None" in {

      val externalEntryUrl = ExternalEntryUrl(None)

      val expectedJson = Json.obj()

      Json.toJson(externalEntryUrl) mustBe expectedJson
    }

    "fail to deserialize if invalid data" in {
      val json = Json.obj(
        "url" -> 12345
      )

      json.validate[ExternalEntryUrl] mustBe a[JsError]
    }

    "deserialize correctly from JSON with null url" in {

      val json = Json.obj(
        "url" -> JsNull
      )

      val expectedExternalEntryUrl = ExternalEntryUrl(None)

      json.validate[ExternalEntryUrl] mustBe JsSuccess(expectedExternalEntryUrl)
    }

    "deserialize correctly from JSON with empty string as url" in {

      val json = Json.obj(
        "url" -> ""
      )

      val expectedExternalEntryUrl = ExternalEntryUrl(Some(""))

      json.validate[ExternalEntryUrl] mustBe JsSuccess(expectedExternalEntryUrl)
    }
  }
}

