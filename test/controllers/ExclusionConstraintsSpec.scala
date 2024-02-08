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

package controllers

import base.SpecBase
import controllers.ExclusionConstraints.{hasActivePaymentWindowExpired, hasActiveReturnWindowExpired}

import java.time.LocalDate

class ExclusionConstraintsSpec extends SpecBase {

  "ExclusionConstraints" - {

    ".hasActiveReturnWindowExpired" - {

      "must return true if active return window has expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3).minusDays(1)
        val result = hasActiveReturnWindowExpired(dueDate, stubClockAtArbitraryDate)

        result mustBe true
      }

      "must return false if active return window is on the day of expiry" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3)
        val result = hasActiveReturnWindowExpired(dueDate, stubClockAtArbitraryDate)

        result mustBe false
      }

      "must return false if active return window has not expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)
        val result = hasActiveReturnWindowExpired(dueDate, stubClockAtArbitraryDate)

        result mustBe false
      }
    }

    ".hasActivePaymentWindowExpired" - {

      "must return true if active payment window has expired" in {

        val paymentDeadlineDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3).minusDays(1)
        val result = hasActivePaymentWindowExpired(paymentDeadlineDate, stubClockAtArbitraryDate)

        result mustBe true
      }

      "must return false if active payment window is on the day of expiry" in {

        val paymentDeadlineDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3)
        val result = hasActivePaymentWindowExpired(paymentDeadlineDate, stubClockAtArbitraryDate)

        result mustBe false
      }

      "must return false if active payment window has not expired" in {

        val paymentDeadlineDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)
        val result = hasActivePaymentWindowExpired(paymentDeadlineDate, stubClockAtArbitraryDate)

        result mustBe false
      }
    }
  }
}
