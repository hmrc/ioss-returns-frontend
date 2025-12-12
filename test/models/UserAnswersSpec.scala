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

package models

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.*
import queries.Settable

import java.time.{Instant, Month}
import scala.util.Failure

class UserAnswersSpec extends SpecBase with Matchers {

  ".UserAnswersForAudit" - {

    "must serialize to JSON correctly" in {
      val userAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value")
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": {
          |    "key": "value"
          |  }
          |}
        """.stripMargin
      )

      val json = Json.toJson(userAnswersForAudit)

      json mustBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": {
          |    "key": "value"
          |  }
          |}
            """.stripMargin
      )

      val expectedUserAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value")
      )

      json.as[UserAnswersForAudit] mustBe expectedUserAnswersForAudit
    }

    "must correctly transform UserAnswers to UserAnswersForAudit" in {
      val userAnswers = UserAnswers(
        id = "testId",
        iossNumber = iossNumber,
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value"),
        lastUpdated = Instant.now
      )

      val userAnswersForAudit = userAnswers.toUserAnswersForAudit

      userAnswersForAudit mustBe UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value")
      )
    }

    "must handle empty data in UserAnswersForAudit" in {
      val userAnswers = UserAnswers(
        id = "testId",
        iossNumber = iossNumber,
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj(),
        lastUpdated = Instant.now
      )

      val userAnswersForAudit = userAnswers.toUserAnswersForAudit

      userAnswersForAudit mustBe UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj()
      )
    }

    "must serialize and deserialize correctly with special characters in data" in {
      val userAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("specialKey" -> """Value with "quotes" and \backslashes""")
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": {
          |    "specialKey": "Value with \"quotes\" and \\backslashes"
          |  }
          |}
        """.stripMargin
      )

      val json = Json.toJson(userAnswersForAudit)
      json mustBe expectedJson

      json.as[UserAnswersForAudit] mustBe userAnswersForAudit
    }

    "must fail deserialization when period is missing" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "data": {
          |    "key": "value"
          |  }
          |}
        """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[UserAnswersForAudit]
      }
    }

    "must fail deserialization when data is invalid" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": "not a json object"
          |}
        """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[UserAnswersForAudit]
      }
    }

    "must serialize and deserialize correctly for January period" in {
      val userAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JANUARY),
        data = Json.obj("key" -> "value")
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M1"
          |  },
          |  "data": {
          |    "key": "value"
          |  }
          |}
        """.stripMargin
      )

      val json = Json.toJson(userAnswersForAudit)
      json mustBe expectedJson

      json.as[UserAnswersForAudit] mustBe userAnswersForAudit
    }

    "must serialize and deserialize correctly for December period" in {
      val userAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.DECEMBER),
        data = Json.obj("key" -> "value")
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M12"
          |  },
          |  "data": {
          |    "key": "value"
          |  }
          |}
        """.stripMargin
      )

      val json = Json.toJson(userAnswersForAudit)
      json mustBe expectedJson

      json.as[UserAnswersForAudit] mustBe userAnswersForAudit
    }

    "must handle lastUpdated field correctly" in {
      val userAnswers = UserAnswers(
        id = "testId",
        iossNumber = iossNumber,
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value"),
        lastUpdated = Instant.parse("2023-06-15T00:00:00Z")
      )

      val userAnswersForAudit = userAnswers.toUserAnswersForAudit

      userAnswersForAudit mustBe UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj("key" -> "value")
      )
    }

    "must handle empty data field correctly" in {
      val userAnswersForAudit = UserAnswersForAudit(
        period = StandardPeriod(2023, Month.JUNE),
        data = Json.obj()
      )

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": {}
          |}
        """.stripMargin
      )

      val json = Json.toJson(userAnswersForAudit)
      json mustBe expectedJson

      json.as[UserAnswersForAudit] mustBe userAnswersForAudit
    }

    "must handle failure during cleanup in remove method" in {
      val page = mock[Settable[String]]
      when(page.path).thenReturn(JsPath \ "key")
      when(page.cleanup(any(), any())).thenReturn(Failure(new Exception("Cleanup failure")))

      val userAnswers = UserAnswers("testId", iossNumber, StandardPeriod(2023, Month.JUNE), Json.obj())
      val result = userAnswers.remove(page)

      result.isFailure mustBe true
      result.failed.get.getMessage must include("Cleanup failure")
    }

    "must fail deserialization when id is missing" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M6"
          |  },
          |  "data": {
          |    "key": "value"
          |  },
          |  "lastUpdated": "2023-06-15T00:00:00Z"
          |}
        """.stripMargin
      )

      intercept[JsResultException] {
        invalidJson.as[UserAnswers]
      }
    }

    "must handle deeply nested JSON in data" in {
      val nestedData = Json.obj(
        "level1" -> Json.obj(
          "level2" -> Json.obj(
            "level3" -> "value"
          )
        )
      )

      val userAnswers = UserAnswers("testId", iossNumber, StandardPeriod(2023, Month.JUNE), nestedData)
      val userAnswersForAudit = userAnswers.toUserAnswersForAudit

      userAnswersForAudit.data mustBe nestedData
    }
  }
}
