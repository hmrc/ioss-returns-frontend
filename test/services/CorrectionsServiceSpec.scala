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

package services

import base.SpecBase
import models.Period

class CorrectionsServiceSpec extends SpecBase {

  private val period1: Period = period
  private val period2: Period = period1.getNext
  private val period3: Period = period2.getNext
  private val period4: Period = period3.getNext

  "CorrectionsService" - {

    ".getAllPeriods" - {

      "must return an empty sequence if no periods within the match range are found" in {

        val periodFrom: Period = period1
        val periodTo: Period = period1

        val service = new CorrectionsService

        val result = service.getAllPeriods(periodFrom, periodTo)

        result mustBe Seq.empty
      }

      "must return a sequence of multiple periods when multiple periods exists within the match range" in {

        val periodFrom: Period = period1
        val periodTo: Period = period4

        val service = new CorrectionsService

        val result = service.getAllPeriods(periodFrom, periodTo)

        result mustBe Seq(period1, period2, period3)
        result mustNot contain theSameElementsAs Seq(period4)
      }

      "must only return a single period when range difference is one period" in {

        val periodFrom: Period = period1
        val periodTo: Period = period2

        val service = new CorrectionsService

        val result = service.getAllPeriods(periodFrom, periodTo)

        result mustBe Seq(period1)
        result mustNot contain theSameElementsAs Seq(period2)
      }
    }
  }
}
