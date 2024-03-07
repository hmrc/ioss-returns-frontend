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

package viewmodels.yourAccount

import base.SpecBase
import models.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import models.{StandardPeriod, SubmissionStatus}
import org.scalatest.EitherValues
import org.scalatest.prop.TableDrivenPropertyChecks

import java.time.YearMonth

class NextReturnCalculationSpec extends SpecBase with EitherValues with TableDrivenPropertyChecks{
  "calculateNonNextReturn must" - {
    "return none if there are no returns" in {
      val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(List.empty)

      exceptionOrReturn match {
        case Right(otherReturn: OtherReturn) =>
          otherReturn mustBe OtherReturn(None, List.empty)
          otherReturn.maybeExpectedCurrentReturn mustBe None
          otherReturn.overdueCount mustBe 0

        case _ => fail(s"expected OtherReturn got $exceptionOrReturn")
      }
    }

    "error when there is something in progress" in {
      val statuses = Table("status", Due, Overdue, Complete, Excluded)

      forAll(statuses){ status =>
        val inProgressReturn = createReturn(status).copy( inProgress = true)
        val returns = List(
          createReturn(Due),
          createReturn(Overdue),
          inProgressReturn,
          createReturn(Due),
          createReturn(Overdue),
        )

        val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(returns)
        exceptionOrReturn.left.map(_.getClass) mustBe Left(classOf[ReturnIsInProgressException])
      }

    }

    "prioritise a return marked as next if it exists" in {
      val nextReturn = createReturn(Next)

      val returns = List(
        createReturn(Due),
        createReturn(Overdue),
        nextReturn,
        createReturn(Due),
        createReturn(Overdue),
      )

      val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(returns)
      exceptionOrReturn.value mustBe NextReturn(nextReturn)
    }

    def createReturn(submissionStatus: SubmissionStatus, year : Int = 2022): Return = {
      Return.fromPeriod(
        period = StandardPeriod.apply(YearMonth.of(year, 10)),
        submissionStatus = submissionStatus,
        inProgress = false,
        isOldest = false
      )
    }

    "prioritise a return marked as Overdue in earliest order" in {
      val firstOverDueReturn =  createReturn(Overdue, 2021)
      val secondOverDueReturn = createReturn(Overdue, 2023)

      val firstDueReturn = createReturn(Due, 2021)
      val secondDueReturn = createReturn(Due, 2023)

      val returns = List(
        secondOverDueReturn,
        firstDueReturn,
        firstOverDueReturn,
        secondDueReturn
      )

      val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(returns)
      exceptionOrReturn match {
        case Right(otherReturn: OtherReturn) =>
          otherReturn mustBe OtherReturn(Some(firstDueReturn), List(secondOverDueReturn, firstOverDueReturn))
          otherReturn.maybeExpectedCurrentReturn mustBe Some(firstOverDueReturn)
          otherReturn.overdueCount mustBe 2

        case _ => fail(s"expected OtherReturn got $exceptionOrReturn")
      }
    }

    "defer to Due when there are no overdue" in {
      val firstDueReturn = createReturn(Due, 2021)
      val secondDueReturn = createReturn(Due, 2023)

      val returns = List(
        firstDueReturn,
        secondDueReturn
      )

      val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(returns)
      exceptionOrReturn match {
        case Right(otherReturn: OtherReturn) =>
          otherReturn mustBe OtherReturn(Some(firstDueReturn), List.empty)
          otherReturn.maybeExpectedCurrentReturn mustBe Some(firstDueReturn)
          otherReturn.overdueCount mustBe 0

        case _ => fail(s"expected OtherReturn got $exceptionOrReturn")
      }
    }
  }


}
