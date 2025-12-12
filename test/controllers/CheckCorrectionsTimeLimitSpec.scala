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

package controllers

import base.SpecBase
import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import models.StandardPeriod

import java.time.{Clock, Instant, LocalDate, ZoneId}

class CheckCorrectionsTimeLimitSpec extends SpecBase {

  ".isOlderThanThreeYears" - {

    val date: LocalDate = LocalDate.of(2023, 1, 1)
    val instant: Instant = date.atStartOfDay(ZoneId.systemDefault).toInstant
    val stubbedClock = Clock.fixed(instant, ZoneId.systemDefault)

    "must return true if period payment deadline (due date) is older than 3 years" in {

      val dateThreeYearsAndTwoMonthsAgo = date.minusYears(3).minusMonths(2)
      val dueDate: LocalDate = StandardPeriod(dateThreeYearsAndTwoMonthsAgo.getYear, dateThreeYearsAndTwoMonthsAgo.getMonth).paymentDeadline

      val result = isOlderThanThreeYears(dueDate, stubbedClock)

      result mustBe true
    }

    "must return false if period payment deadline (due date) is not older than 3 years" in {

      val dateThreeYearsAndOneMonthAgo = date.minusYears(3).plusMonths(1)
      val dueDate: LocalDate = StandardPeriod(dateThreeYearsAndOneMonthAgo.getYear, dateThreeYearsAndOneMonthAgo.getMonth).paymentDeadline

      val result = isOlderThanThreeYears(dueDate, stubbedClock)

      result mustBe false
    }
  }
}
