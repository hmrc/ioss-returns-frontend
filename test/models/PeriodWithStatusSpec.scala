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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsResultException, Json}

import java.time.{DateTimeException, LocalDate, Month}


class PeriodWithStatusSpec
  extends AnyFreeSpec with Matchers{

  "PeriodWithStatus" - {

    "serialize and deserialize correctly" in {

      val period = StandardPeriod(2023, Month.JANUARY)
      val status = SubmissionStatus.Due
      val periodWithStatus = PeriodWithStatus(period, status)

      val json = Json.toJson(periodWithStatus)

      val expectedJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M1"
          |  },
          |  "status": "DUE"
          |}
            """.stripMargin
      )

      json mustBe expectedJson

      val deserialized = json.as[PeriodWithStatus]

      deserialized mustBe periodWithStatus
    }

    "handle different SubmissionStatus values" in {
      val statuses = SubmissionStatus.values
      statuses.foreach { status =>
        val period = StandardPeriod(2023, Month.FEBRUARY)
        val periodWithStatus = PeriodWithStatus(period, status)

        val json = Json.toJson(periodWithStatus)
        val deserialized = json.as[PeriodWithStatus]

        deserialized mustBe periodWithStatus
      }
    }

    "fail deserialization for invalid JSON" in {
      val invalidJson = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M13"
          |  },
          |  "status": "INVALID_STATUS"
          |}
            """.stripMargin
      )

      val exception = intercept[DateTimeException] {
        invalidJson.as[PeriodWithStatus]
      }

      exception.getMessage must include("Invalid value for MonthOfYear")
    }

    "handle edge cases for StandardPeriod" in {
      val janPeriod = StandardPeriod(2023, Month.JANUARY)
      val decPeriod = StandardPeriod(2023, Month.DECEMBER)

      janPeriod.firstDay mustBe LocalDate.of(2023, 1, 1)
      janPeriod.lastDay mustBe LocalDate.of(2023, 1, 31)
      janPeriod.isPartial mustBe false

      decPeriod.firstDay mustBe LocalDate.of(2023, 12, 1)
      decPeriod.lastDay mustBe LocalDate.of(2023, 12, 31)
      decPeriod.isPartial mustBe false
    }

    "verify ordering of Periods" in {
      val period1 = StandardPeriod(2023, Month.JANUARY)
      val period2 = StandardPeriod(2023, Month.FEBRUARY)
      val period3 = StandardPeriod(2024, Month.JANUARY)

      Seq(period3, period1, period2).sorted mustBe Seq(period1, period2, period3)
    }

  }

  "Json.format[PeriodWithStatus]" - {

    "serialize and deserialize successfully" in {
      val period = StandardPeriod(2023, Month.APRIL)
      val status = SubmissionStatus.Overdue
      val model = PeriodWithStatus(period, status)

      val json = Json.toJson(model)

      val deserialized = json.as[PeriodWithStatus]

      deserialized mustBe model
    }

    "fail deserialization when 'period' is missing" in {
      val json = Json.parse(
        """
          |{
          |  "status": "COMPLETE"
          |}
          """.stripMargin
      )

      intercept[JsResultException] {
        json.as[PeriodWithStatus]
      }
    }

    "fail deserialization when 'status' is missing" in {
      val json = Json.parse(
        """
          |{
          |  "period": {
          |    "year": 2023,
          |    "month": "M4"
          |  }
          |}
          """.stripMargin
      )

      intercept[JsResultException] {
        json.as[PeriodWithStatus]
      }
    }

    "fail deserialization for empty JSON" in {
      val emptyJson = Json.parse("{}")

      intercept[JsResultException] {
        emptyJson.as[PeriodWithStatus]
      }
    }
  }

}
