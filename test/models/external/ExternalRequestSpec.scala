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
import play.api.libs.json.{JsValue, Json}


class ExternalRequestSpec extends SpecBase {

  "ExternalRequest" - {

    "serialize correctly to JSON" in {

      val externalRequest = ExternalRequest("http://origin.com", "http://return.com")
      val json: JsValue = Json.toJson(externalRequest)

      json mustBe Json.parse("""{"origin":"http://origin.com","returnUrl":"http://return.com"}""")
    }

    "deserialize correctly from JSON" in {

      val json: JsValue = Json.parse("""{"origin":"http://origin.com","returnUrl":"http://return.com"}""")
      val externalRequest = json.as[ExternalRequest]

      externalRequest mustBe ExternalRequest("http://origin.com", "http://return.com")
    }

    "fail to deserialize if a required field is missing" in {
      val json: JsValue = Json.parse("""{"origin":"http://origin.com"}""")

      intercept[Exception] {
        json.as[ExternalRequest]
      }
    }

    "handle extra fields gracefully during deserialization" in {
      val json: JsValue = Json.parse(
        """{"origin":"http://origin.com","returnUrl":"http://return.com","extraField":"extraValue"}"""
      )
      val externalRequest = json.as[ExternalRequest]

      externalRequest mustBe ExternalRequest("http://origin.com", "http://return.com")
    }
  }
}

