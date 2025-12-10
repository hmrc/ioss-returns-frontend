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

package models.core

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}

import java.time.LocalDate

class CoreRegistrationValidationResultSpec extends AnyFreeSpec with Matchers with SpecBase {

  "CoreRegistrationValidationResult" - {

    "must serialise and deserialise to and from a CoreRegistrationValidationResult" - {

      "with all optional fields present" in {

        val coreRegistrationValidationResult: CoreRegistrationValidationResult =
          CoreRegistrationValidationResult(
            "IM2344433220",
            Some("IN4747493822"),
            "FR",
            traderFound = true,
            Seq(Match(
              TraderId("IM0987654321"),
              Some("444444444"),
              "DE",
              Some(3),
              Some(LocalDate.now().toString),
              Some(LocalDate.now().toString),
              Some(1),
              Some(2)
            ))
          )

        val expectedJson = Json.obj(
          "searchId" -> "IM2344433220",
          "searchIdIntermediary" -> "IN4747493822",
          "searchIdIssuedBy" -> "FR",
          "traderFound" -> true,
          "matches" -> Json.arr(
            Json.obj(
              "traderId" -> "IM0987654321",
              "intermediary" -> "444444444",
              "memberState" -> "DE",
              "exclusionStatusCode" -> 3,
              "exclusionDecisionDate" -> s"${LocalDate.now()}",
              "exclusionEffectiveDate" -> s"${LocalDate.now()}",
              "nonCompliantReturns" -> 1,
              "nonCompliantPayments" -> 2
            ))
        )

        Json.toJson(coreRegistrationValidationResult) mustEqual expectedJson
        expectedJson.validate[CoreRegistrationValidationResult] mustEqual JsSuccess(coreRegistrationValidationResult)
      }

      "with all optional fields missing" in {
        val coreRegistrationValidationResult: CoreRegistrationValidationResult =
          CoreRegistrationValidationResult(
            "IM2344433220",
            None,
            "FR",
            traderFound = true,
            Seq(Match(
              TraderId("IM0987654321"),
              None,
              "DE",
              None,
              None,
              None,
              None,
              None
            ))
          )

        val expectedJson = Json.obj(
          "searchId" -> "IM2344433220",
          "searchIdIssuedBy" -> "FR",
          "traderFound" -> true,
          "matches" -> Json.arr(
            Json.obj(
              "traderId" -> "IM0987654321",
              "memberState" -> "DE"
            ))
        )

        Json.toJson(coreRegistrationValidationResult) mustEqual expectedJson
        expectedJson.validate[CoreRegistrationValidationResult] mustEqual JsSuccess(coreRegistrationValidationResult)
      }

    }
  }

}
