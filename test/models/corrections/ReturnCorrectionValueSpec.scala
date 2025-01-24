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

package models.corrections

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}


class ReturnCorrectionValueSpec extends AnyFreeSpec with Matchers with SpecBase {


  "ReturnCorrectionValue" - {

    "must serialize to JSON correctly" in {

      val returnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = 400
      )

      val expectedJson = Json.obj(
        "maximumCorrectionValue" -> 400
      )

      Json.toJson(returnCorrectionValue) mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {

      val json = Json.obj(
        "maximumCorrectionValue" -> 400
      )

      val expectedReturnCorrectionValue = ReturnCorrectionValue(
        maximumCorrectionValue = 400
      )

      json.validate[ReturnCorrectionValue] mustBe JsSuccess(expectedReturnCorrectionValue)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[ReturnCorrectionValue] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "maximumCorrectionValue" -> "test"
      )

      json.validate[ReturnCorrectionValue] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "maximumCorrectionValue" -> JsNull
      )

      json.validate[ReturnCorrectionValue] mustBe a[JsError]
    }
  }

}
