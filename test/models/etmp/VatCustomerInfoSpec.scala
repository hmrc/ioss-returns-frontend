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

import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}
import base.SpecBase

import java.time.LocalDate

class VatCustomerInfoSpec extends SpecBase {

  "VatCustomerInfo" - {

    "must deserialise" - {

      "when all optional fields are present" in {

        val json = Json.obj(
          "desAddress" -> Json.obj(
            "line1" -> "line 1",
            "line2" -> "line 2",
            "line3" -> "line 3",
            "line4" -> "line 4",
            "line5" -> "line 5",
            "postCode" -> "postcode",
            "countryCode" -> "CC"
          ),
          "registrationDate" -> "2020-01-02",
          "partOfVatGroup" -> true,
          "organisationName" -> "Foo",
          "individualName" -> "A B C",
          "singleMarketIndicator" -> false,
          "deregistrationDecisionDate" -> "2022-08-21",
          "overseasIndicator" -> false
        )

        val expectedResult = VatCustomerInfo(
          desAddress = DesAddress("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("line 5"), Some("postcode"), "CC"),
          registrationDate = Some(LocalDate.of(2020, 1, 2)),
          partOfVatGroup = true,
          organisationName = Some("Foo"),
          singleMarketIndicator = false,
          individualName = Some("A B C"),
          deregistrationDecisionDate = Some(LocalDate.of(2022, 8, 21)),
          overseasIndicator = false
        )

        json.validate[VatCustomerInfo] mustBe JsSuccess(expectedResult)
      }

      "when all optional fields are present and partOfVatGroup is false" in {

        val json = Json.obj(
          "desAddress" -> Json.obj(
            "line1" -> "line 1",
            "line2" -> "line 2",
            "line3" -> "line 3",
            "line4" -> "line 4",
            "line5" -> "line 5",
            "postCode" -> "postcode",
            "countryCode" -> "CC"
          ),
          "registrationDate" -> "2020-01-02",
          "partOfVatGroup" -> false,
          "organisationName" -> "Foo",
          "individualName" -> "A B C",
          "singleMarketIndicator" -> false,
          "deregistrationDecisionDate" -> "2022-08-21",
          "overseasIndicator" -> false
        )

        val expectedResult = VatCustomerInfo(
          desAddress = DesAddress("line 1", Some("line 2"), Some("line 3"), Some("line 4"), Some("line 5"), Some("postcode"), "CC"),
          registrationDate = Some(LocalDate.of(2020, 1, 2)),
          partOfVatGroup = false,
          organisationName = Some("Foo"),
          singleMarketIndicator = false,
          individualName = Some("A B C"),
          deregistrationDecisionDate = Some(LocalDate.of(2022, 8, 21)),
          overseasIndicator = false
        )

        json.validate[VatCustomerInfo] mustBe JsSuccess(expectedResult)
      }

      "when all optional fields are absent" in {

        val json = Json.obj(
          "desAddress" -> Json.obj(
            "line1" -> "line 1",
            "countryCode" -> "CC"
          ),
          "registrationDate" -> JsNull,
          "partOfVatGroup" -> false,
          "organisationName" -> JsNull,
          "individualName" -> JsNull,
          "singleMarketIndicator" -> false,
          "deregistrationDecisionDate" -> JsNull,
          "overseasIndicator" -> false
        )

        val expectedResult = VatCustomerInfo(
          desAddress = DesAddress("line 1", None, None, None, None, None, "CC"),
          registrationDate = None,
          partOfVatGroup = false,
          organisationName = None,
          singleMarketIndicator = false,
          individualName = None,
          deregistrationDecisionDate = None,
          overseasIndicator = false
        )

        json.validate[VatCustomerInfo] mustBe JsSuccess(expectedResult)
      }
    }
  }

  "DesAddress" - {

    "must deserialise from JSON when all optional fields are present" in {

      val json = Json.obj(
        "line1" -> "line 1",
        "line2" -> "line 2",
        "line3" -> "line 3",
        "line4" -> "line 4",
        "line5" -> "line 5",
        "postCode" -> "postcode",
        "countryCode" -> "CC"
      )

      val expectedResult = DesAddress(
        line1 = "line 1",
        line2 = Some("line 2"),
        line3 = Some("line 3"),
        line4 = Some("line 4"),
        line5 = Some("line 5"),
        postCode = Some("postcode"),
        countryCode = "CC"
      )

      json.validate[DesAddress] mustBe JsSuccess(expectedResult)
    }

    "must deserialise from JSON when some optional fields are missing" in {

      val json = Json.obj(
        "line1" -> "line 1",
        "line3" -> "line 3",
        "countryCode" -> "CC"
      )

      val expectedResult = DesAddress(
        line1 = "line 1",
        line2 = None,
        line3 = Some("line 3"),
        line4 = None,
        line5 = None,
        postCode = None,
        countryCode = "CC"
      )

      json.validate[DesAddress] mustBe JsSuccess(expectedResult)
    }

    "must fail to deserialise when required fields are missing" in {

      val json = Json.obj(
        "line2" -> "line 2",
        "line3" -> "line 3",
        "countryCode" -> "CC"
      )

      json.validate[DesAddress] mustBe a[JsError]
    }

    "must serialise to JSON correctly" in {

      val address = DesAddress(
        line1 = "line 1",
        line2 = Some("line 2"),
        line3 = Some("line 3"),
        line4 = None,
        line5 = None,
        postCode = Some("postcode"),
        countryCode = "CC"
      )

      val expectedJson = Json.obj(
        "line1" -> "line 1",
        "line2" -> "line 2",
        "line3" -> "line 3",
        "postCode" -> "postcode",
        "countryCode" -> "CC"
      )

      Json.toJson(address) mustBe expectedJson
    }

    "must serialise to JSON correctly when optional fields are None" in {

      val address = DesAddress(
        line1 = "line 1",
        line2 = None,
        line3 = None,
        line4 = None,
        line5 = None,
        postCode = None,
        countryCode = "CC"
      )

      val expectedJson = Json.obj(
        "line1" -> "line 1",
        "countryCode" -> "CC"
      )

      Json.toJson(address) mustBe expectedJson
    }
  }
}

