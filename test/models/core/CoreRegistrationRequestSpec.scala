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

package models.core

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{Json, Reads, Writes}


class CoreRegistrationRequestSpec extends AnyFreeSpec with Matchers with SpecBase {

  val coreRegistrationRequest: CoreRegistrationRequest = CoreRegistrationRequest(
    source = "VAT",
    scheme = Some("OSS"),
    searchId = "12345",
    searchIntermediary = Some("IntermediaryA"),
    searchIdIssuedBy = "DE"
  )

  "CoreRegistrationRequest" - {

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(coreRegistrationRequest)
      val deserializedObject = serializedJson.as[CoreRegistrationRequest]

      deserializedObject mustEqual coreRegistrationRequest
    }

    "handle optional fields (None value)" in {
      val coreRegistrationRequestWithoutScheme = coreRegistrationRequest.copy(scheme = None)
      val coreRegistrationRequestWithoutIntermediary = coreRegistrationRequest.copy(searchIntermediary = None)

      val serializedJsonWithoutScheme = Json.toJson(coreRegistrationRequestWithoutScheme)
      val serializedJsonWithoutIntermediary = Json.toJson(coreRegistrationRequestWithoutIntermediary)

      (serializedJsonWithoutScheme \ "scheme").asOpt[String] mustBe None
      (serializedJsonWithoutIntermediary \ "searchIntermediary").asOpt[String] mustBe None
    }

    "handle optional fields (Some value)" in {
      val coreRegistrationRequestWithScheme = coreRegistrationRequest.copy(scheme = Some("OSS"))
      val coreRegistrationRequestWithIntermediary = coreRegistrationRequest.copy(searchIntermediary = Some("IntermediaryB"))

      val serializedJsonWithScheme = Json.toJson(coreRegistrationRequestWithScheme)
      val serializedJsonWithIntermediary = Json.toJson(coreRegistrationRequestWithIntermediary)

      (serializedJsonWithScheme \ "scheme").asOpt[String] mustBe Some("OSS")
      (serializedJsonWithIntermediary \ "searchIntermediary").asOpt[String] mustBe Some("IntermediaryB")
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "source" -> "VAT",
        "scheme" -> 123,
        "searchId" -> "12345",
        "searchIdIssuedBy" -> "DE"
      )

      val result = invalidJson.validate[CoreRegistrationRequest]
      result.isError mustBe true
    }
  }

}
