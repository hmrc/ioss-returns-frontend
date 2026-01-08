/*
 * Copyright 2026 HM Revenue & Customs
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
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EtmpWebsiteSpec extends SpecBase {

  "EtmpWebsite" - {

    "must serialise/ to JSON correctly" in {

      val website = arbitrary[EtmpWebsite].sample.value

      val expectedJson = Json.obj(
        "websiteAddress" -> s"${website.websiteAddress}"
      )

      Json.toJson(website) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "websiteAddress" -> "http://example.com"
      )

      val expectedWebsite = EtmpWebsite("http://example.com")

      json.validate[EtmpWebsite] mustBe JsSuccess(expectedWebsite)
    }

    "must handle missing fields during deserialization" in {
      val json = Json.obj()

      json.validate[EtmpWebsite] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val json = Json.obj(
        "websiteAddress" -> 12345
      )

      json.validate[EtmpWebsite] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {
      val json = Json.obj(
        "websiteAddress" -> JsNull
      )

      json.validate[EtmpWebsite] mustBe a[JsError]
    }
  }
}
