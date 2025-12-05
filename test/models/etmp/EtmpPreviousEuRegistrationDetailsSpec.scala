/*
 * Copyright 2023 HM Revenue & Customs
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
import models.Country
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EtmpPreviousEuRegistrationDetailsSpec extends SpecBase {

  private val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  "EtmpPreviousEuRegistrationDetails" - {

    "must deserialise/serialise to and from EtmpPreviousEuRegistrationDetails" - {

      "when all optional values are present" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> etmpEuPreviousRegistrationDetails.registrationNumber,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType,
          "intermediaryNumber" -> etmpEuPreviousRegistrationDetails.intermediaryNumber
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = etmpEuPreviousRegistrationDetails.issuedBy,
          registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber,
          schemeType = etmpEuPreviousRegistrationDetails.schemeType,
          intermediaryNumber = etmpEuPreviousRegistrationDetails.intermediaryNumber
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "when all optional values are absent" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> etmpEuPreviousRegistrationDetails.registrationNumber,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType,
        )

        val expectedResult = EtmpPreviousEuRegistrationDetails(
          issuedBy = etmpEuPreviousRegistrationDetails.issuedBy,
          registrationNumber = etmpEuPreviousRegistrationDetails.registrationNumber,
          schemeType = etmpEuPreviousRegistrationDetails.schemeType
        )

        Json.toJson(expectedResult) mustBe json
        json.validate[EtmpPreviousEuRegistrationDetails] mustBe JsSuccess(expectedResult)
      }

      "must handle missing fields during deserialization" in {

        val json = Json.obj()

        json.validate[EtmpPreviousEuRegistrationDetails] mustBe a[JsError]
      }

      "must handle invalid data during deserialization" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> 12345,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType,
          "intermediaryNumber" -> intermediaryNumber
        )

        json.validate[EtmpPreviousEuRegistrationDetails] mustBe a[JsError]
      }

      "must handle null data during deserialization" in {

        val json = Json.obj(
          "issuedBy" -> etmpEuPreviousRegistrationDetails.issuedBy,
          "registrationNumber" -> JsNull,
          "schemeType" -> etmpEuPreviousRegistrationDetails.schemeType,
          "intermediaryNumber" -> intermediaryNumber
        )

        json.validate[EtmpPreviousEuRegistrationDetails] mustBe a[JsError]
      }
    }
  }
}

