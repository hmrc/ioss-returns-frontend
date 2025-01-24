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
import play.api.libs.json.{JsError, JsValue, Json, OFormat}


class ExternalResponseSpec extends SpecBase {

  "ExternalResponse" - {

    "serialize correctly to JSON" in {

      val externalResponse = ExternalResponse("http://example.com")
      val json: JsValue = Json.toJson(externalResponse)

      json mustBe Json.parse("""{"redirectUrl":"http://example.com"}""")

    }

    "deserialize correctly from JSON" in {

      val json: JsValue = Json.parse("""{"redirectUrl":"http://example.com"}""")
      val externalResponse = json.as[ExternalResponse]

      externalResponse mustBe ExternalResponse("http://example.com")
    }

    "handle missing redirectUrl during deserialization" in {

      val json: JsValue = Json.parse("""{}""")

      intercept[Exception] {
        json.as[ExternalResponse]
      }

    }

    "handle extra fields gracefully during deserialization" in {

      val json: JsValue = Json.parse(
        """{"redirectUrl":"http://example.com", "extraField":"extraValue"}"""
      )

      val externalResponse = json.as[ExternalResponse]

      externalResponse mustBe ExternalResponse("http://example.com")

    }

    "serialize and deserialize back to the same ExternalResponse object" in {
      val externalResponse = ExternalResponse("http://example.com")
      val json = Json.toJson(externalResponse)

      val deserialized = json.as[ExternalResponse]

      deserialized mustBe externalResponse
    }

    "produce expected JSON structure when serialized" in {
      val externalResponse = ExternalResponse("http://example.com")

      val json = Json.toJson(externalResponse)

      (json \ "redirectUrl").asOpt[String] mustBe Some("http://example.com")
    }

    "fail gracefully when JSON is completely invalid" in {
      val json: JsValue = Json.parse("""{"unknownField":"unknownValue"}""")

      val result = json.validate[ExternalResponse]

      result mustBe a[JsError]
    }

    "serialize correctly when redirectUrl contains query parameters" in {
      val externalResponse = ExternalResponse("http://example.com?param=value")
      val json: JsValue = Json.toJson(externalResponse)

      json mustBe Json.parse("""{"redirectUrl":"http://example.com?param=value"}""")
    }

    "serialize and deserialize a deeply nested structure correctly" in {
      case class NestedResponse(wrapper: ExternalResponse)
      implicit val nestedResponseFormat: OFormat[NestedResponse] = Json.format[NestedResponse]

      val nestedResponse = NestedResponse(ExternalResponse("http://example.com"))
      val json = Json.toJson(nestedResponse)

      json mustBe Json.parse("""{"wrapper":{"redirectUrl":"http://example.com"}}""")
      json.as[NestedResponse] mustBe nestedResponse
    }
  }
}

