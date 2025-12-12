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

package models.financialdata

import base.SpecBase
import play.api.libs.json.{JsError, Json}

class ItemSpec extends SpecBase {

  private val itemJson =
    """{
      |         "amount": 500,
      |         "clearingReason": "",
      |         "paymentReference": "",
      |         "paymentAmount": 500,
      |         "paymentMethod": ""
      |       }""".stripMargin

  private val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))

  "Item" - {
    "must deserialise correctly" in {
      Json.parse(itemJson).as[Item] mustBe item
    }

    "must serialise correctly`" in {
      Json.toJson(item) mustBe Json.parse(itemJson)
    }

    "must deserialise correctly with missing optional fields" in {
      val jsonWithMissingFields =
        """{
          |  "amount": 500,
          |  "paymentAmount": 500
          |}""".stripMargin

      val expectedItem = Item(
        amount = Some(500),
        clearingReason = None,
        paymentReference = None,
        paymentAmount = Some(500),
        paymentMethod = None
      )

      Json.parse(jsonWithMissingFields).as[Item] mustBe expectedItem
    }

    "must serialise correctly with missing optional fields" in {
      val itemWithMissingFields = Item(
        amount = Some(500),
        clearingReason = None,
        paymentReference = None,
        paymentAmount = Some(500),
        paymentMethod = None
      )

      val expectedJson = Json.parse(
        """{
          |  "amount": 500,
          |  "paymentAmount": 500
          |}""".stripMargin
      )

      Json.toJson(itemWithMissingFields) mustBe expectedJson
    }

    "must deserialise correctly with null fields" in {
      val jsonWithNullFields =
        """{
          |  "amount": 500,
          |  "clearingReason": null,
          |  "paymentReference": null,
          |  "paymentAmount": 500,
          |  "paymentMethod": null
          |}""".stripMargin

      val expectedItem = Item(
        amount = Some(500),
        clearingReason = None,
        paymentReference = None,
        paymentAmount = Some(500),
        paymentMethod = None
      )

      Json.parse(jsonWithNullFields).as[Item] mustBe expectedItem
    }

    "must fail to deserialise with invalid data types" in {
      val invalidJson =
        """{
          |  "amount": "invalid-amount",
          |  "paymentAmount": "invalid-payment-amount"
          |}""".stripMargin

      Json.parse(invalidJson).validate[Item] mustBe a[JsError]
    }

    "must serialise and deserialise correctly with all fields populated" in {
      val fullyPopulatedItem = Item(
        amount = Some(1000),
        clearingReason = Some("Cleared due to payment"),
        paymentReference = Some("REF123"),
        paymentAmount = Some(1000),
        paymentMethod = Some("Bank Transfer")
      )

      val json = Json.parse(
        """{
          |  "amount": 1000,
          |  "clearingReason": "Cleared due to payment",
          |  "paymentReference": "REF123",
          |  "paymentAmount": 1000,
          |  "paymentMethod": "Bank Transfer"
          |}""".stripMargin
      )

      Json.toJson(fullyPopulatedItem) mustBe json
      json.as[Item] mustBe fullyPopulatedItem
    }

    "must serialise correctly with all fields as None" in {
      val itemWithAllNone = Item(
        amount = None,
        clearingReason = None,
        paymentReference = None,
        paymentAmount = None,
        paymentMethod = None
      )

      val expectedJson = Json.obj()

      Json.toJson(itemWithAllNone) mustBe expectedJson
    }
  }
}
