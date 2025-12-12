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
import play.api.libs.json.{JsError, JsSuccess, Json}

class TraderIdSpec extends SpecBase {

  "TraderId" - {

    "deserialize/serialize to and from VatNumberTraderId" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")

      val expectedJson = Json.obj(
        "vatNumber" -> "GB123456789"
      )

      Json.toJson(vatNumberTraderId) mustBe expectedJson

      expectedJson.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "deserialize/serialize to and from TaxRefTraderID" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> "TR123456789"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson

      expectedJson.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }

    "correctly serialize and deserialize VatNumberTraderId" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")

      val json = Json.toJson(vatNumberTraderId)

      json.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "correctly serialize and deserialize TaxRefTraderID" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")

      val json = Json.toJson(taxRefTraderID)

      json.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }

    "handle unknown fields gracefully during deserialization" in {
      val json = Json.obj(
        "unknownField" -> "SomeValue"
      )

      json.validate[TraderId] mustBe a[JsError]
    }

    "deserialize from valid VatNumberTraderId JSON" in {
      val json = Json.obj(
        "vatNumber" -> "GB123456789"
      )

      json.validate[TraderId] mustBe JsSuccess(VatNumberTraderId("GB123456789"))
    }

    "deserialize from valid TaxRefTraderID JSON" in {
      val json = Json.obj(
        "taxReferenceNumber" -> "TR123456789"
      )

      json.validate[TraderId] mustBe JsSuccess(TaxRefTraderID("TR123456789"))
    }

    "serialize and deserialize correctly from VatNumberTraderId JSON to TraderId" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")
      val json = Json.toJson(vatNumberTraderId)

      json.validate[TraderId] mustBe JsSuccess(vatNumberTraderId)
    }

    "serialize and deserialize correctly from TaxRefTraderID JSON to TraderId" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")
      val json = Json.toJson(taxRefTraderID)

      json.validate[TraderId] mustBe JsSuccess(taxRefTraderID)
    }

    "handle deserialization from a JSON representing either VatNumberTraderId or TaxRefTraderID" in {
      val vatNumberJson = Json.obj("vatNumber" -> "GB123456789")
      val taxRefJson = Json.obj("taxReferenceNumber" -> "TR123456789")

      vatNumberJson.validate[TraderId] mustBe JsSuccess(VatNumberTraderId("GB123456789"))
      taxRefJson.validate[TraderId] mustBe JsSuccess(TaxRefTraderID("TR123456789"))
    }

    "fail to deserialize if the JSON doesn't match either VatNumberTraderId or TaxRefTraderID" in {
      val invalidJson = Json.obj("someOtherField" -> "value")

      invalidJson.validate[TraderId] mustBe a[JsError]
    }

    "serialize VatNumberTraderId using VatNumberTraderId.format" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")

      val json = TraderId.writes.writes(vatNumberTraderId)

      json mustBe Json.toJson(vatNumberTraderId)(VatNumberTraderId.format)
    }

    "serialize TaxRefTraderID using TaxRefTraderID.format" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")

      val json = TraderId.writes.writes(taxRefTraderID)

      json mustBe Json.toJson(taxRefTraderID)(TaxRefTraderID.format)
    }
  }

  "VatNumberTraderId" - {

    "serialize correctly to JSON" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")

      val expectedJson = Json.obj(
        "vatNumber" -> "GB123456789"
      )

      Json.toJson(vatNumberTraderId) mustBe expectedJson
    }

    "deserialize correctly from valid JSON" in {
      val json = Json.obj(
        "vatNumber" -> "GB123456789"
      )

      json.validate[VatNumberTraderId] mustBe JsSuccess(VatNumberTraderId("GB123456789"))
    }

    "fail to deserialize from invalid JSON (missing vatNumber)" in {
      val json = Json.obj(
        "taxReferenceNumber" -> "TR123456789"
      )

      json.validate[VatNumberTraderId] mustBe a[JsError]
    }

    "fail to deserialize from invalid JSON (wrong field type)" in {
      val json = Json.obj(
        "vatNumber" -> 123456789
      )

      json.validate[VatNumberTraderId] mustBe a[JsError]
    }

    "properly use the Json.format for VatNumberTraderId" in {
      val vatNumberTraderId = VatNumberTraderId("GB123456789")

      val json = Json.toJson(vatNumberTraderId)
      (json \ "vatNumber").asOpt[String] mustBe Some("GB123456789")
    }
  }

  "TaxRefTraderID" - {

    "serialize correctly to JSON" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")

      val expectedJson = Json.obj(
        "taxReferenceNumber" -> "TR123456789"
      )

      Json.toJson(taxRefTraderID) mustBe expectedJson
    }

    "deserialize correctly from valid JSON" in {
      val json = Json.obj(
        "taxReferenceNumber" -> "TR123456789"
      )

      json.validate[TaxRefTraderID] mustBe JsSuccess(TaxRefTraderID("TR123456789"))
    }

    "fail to deserialize from invalid JSON (missing taxReferenceNumber)" in {
      val json = Json.obj(
        "vatNumber" -> "GB123456789"
      )

      json.validate[TaxRefTraderID] mustBe a[JsError]
    }

    "fail to deserialize from invalid JSON (wrong field type)" in {
      val json = Json.obj(
        "taxReferenceNumber" -> 123456789
      )

      json.validate[TaxRefTraderID] mustBe a[JsError]
    }

    "properly use the Json.format for TaxRefTraderID" in {
      val taxRefTraderID = TaxRefTraderID("TR123456789")

      val json = Json.toJson(taxRefTraderID)
      (json \ "taxReferenceNumber").asOpt[String] mustBe Some("TR123456789")
    }
  }
}

