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

import java.time.LocalDateTime

class EtmpAdminUseSpec extends SpecBase {

  private val changeDate: LocalDateTime = LocalDateTime.now(stubClockAtArbitraryDate).withSecond(1)

  "EtmpAdminUse" - {

    "must serialise/deserialise to and from EtmpAdminUse" in {

      val json = Json.obj(
        "changeDate" -> s"$changeDate"
      )

      val expectedResult = EtmpAdminUse(changeDate = Some(changeDate))

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAdminUse] mustBe JsSuccess(expectedResult)
    }

    "when all optional values are absent" in {

      val json = Json.obj()

      val expectedResult = EtmpAdminUse(
        changeDate = None
      )

      Json.toJson(expectedResult) mustBe json
      json.validate[EtmpAdminUse] mustBe JsSuccess(expectedResult)
    }

    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "changeDate" -> "invalid data"
      )

      json.validate[EtmpAdminUse] mustBe a[JsError]
    }
  }
}