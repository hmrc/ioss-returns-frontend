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

package models.saveForLater

import base.SpecBase
import play.api.libs.json.{JsError, JsSuccess, Json}

class SavedUserAnswersSpec extends SpecBase {

  private val savedUserAnswers: SavedUserAnswers = arbitrarySavedUserAnswers.arbitrary.sample.value

  "SavedUserAnswers" - {

    "must deserialise/serialise from and to SavedUserAnswers" in {

      val json = Json.obj(
        "iossNumber" -> savedUserAnswers.iossNumber,
        "period" -> savedUserAnswers.period,
        "data" -> savedUserAnswers.data,
        "lastUpdated" -> savedUserAnswers.lastUpdated
      )

      val expectedResult: SavedUserAnswers = SavedUserAnswers(
        iossNumber = savedUserAnswers.iossNumber,
        period = savedUserAnswers.period,
        data = savedUserAnswers.data,
        lastUpdated = savedUserAnswers.lastUpdated
      )

      Json.toJson(expectedResult) `mustBe` json
      json.validate[SavedUserAnswers] `mustBe` JsSuccess(expectedResult)
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[SavedUserAnswers] `mustBe` a[JsError]
    }

    "must handle invalid fields during deserialization" in {

      val json = Json.obj(
        "iossNumber" -> savedUserAnswers.iossNumber,
        "period" -> 1234567,
        "data" -> savedUserAnswers.data,
        "lastUpdated" -> savedUserAnswers.lastUpdated
      )

      json.validate[SavedUserAnswers] `mustBe` a[JsError]
    }
  }
}
