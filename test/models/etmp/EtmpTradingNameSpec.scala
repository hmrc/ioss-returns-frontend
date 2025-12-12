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
import org.scalacheck.Arbitrary.arbitrary
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EtmpTradingNameSpec extends SpecBase {

  "EtmpTradingName" - {

    "must serialise/deserialise to and from EtmpTradingName" in {

      val etmpTradingName = arbitrary[EtmpTradingName].sample.value

      val expectedJson = Json.obj(
        "tradingName" -> s"${etmpTradingName.tradingName}"
      )

      Json.toJson(etmpTradingName) mustBe expectedJson
      expectedJson.validate[EtmpTradingName] mustBe JsSuccess(etmpTradingName)
    }

    "must deserialize from JSON correctly" in {
      val expectedJson = Json.obj(
        "tradingName" -> "Trading Name"
      )

      val expectedTradingName = EtmpTradingName("Trading Name")

      expectedJson.validate[EtmpTradingName] mustBe JsSuccess(expectedTradingName)
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[EtmpTradingName] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val expectedJson = Json.obj(
        "tradingName" -> 12345
      )

      expectedJson.validate[EtmpTradingName] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {
      val expectedJson = Json.obj(
        "tradingName" -> JsNull
      )

      expectedJson.validate[EtmpTradingName] mustBe a[JsError]
    }
  }
}
