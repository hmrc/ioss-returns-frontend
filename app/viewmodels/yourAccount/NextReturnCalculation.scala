/*
 * Copyright 2026 HM Revenue & Customs
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

import models.SubmissionStatus.{Due, Next, Overdue}

sealed trait ActionableReturn

case class NextReturn(pendingReturn: Return) extends ActionableReturn

case class OtherReturn(maybeDueReturn: Option[Return], overDueReturns: Seq[Return])
  extends ActionableReturn {

  val overdueCount: Int = overDueReturns.size

  val maybeExpectedCurrentReturn: Option[Return] =
    overDueReturns
      .sortBy(_.period.firstDay)
      .headOption
      .map(Some.apply)
      .getOrElse(maybeDueReturn)

}

sealed case class ReturnIsInProgressException(message : String)  extends RuntimeException(message)

object NextReturnCalculation {

  def calculateNonNextReturn(returns: Seq[Return]): Either[RuntimeException, ActionableReturn] = {
    val maybeNextReturn = returns.find(_.submissionStatus == Next)
    val maybeInProgressReturn = returns.find(_.inProgress)
    val maybeDueReturn = returns.find(_.submissionStatus == Due)
    val overdueReturns = returns.filter(_.submissionStatus == Overdue)

    maybeNextReturn.map { nextReturn =>
      Right(NextReturn(nextReturn))
    }.getOrElse {
      if (maybeInProgressReturn.isDefined) {
        Left(ReturnIsInProgressException("There should be no in progress returns"))
      } else {
        Right(OtherReturn(maybeDueReturn, overdueReturns))
      }
    }

  }

}
