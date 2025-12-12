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

package pages.corrections

import models.{Index, UserAnswers}
import pages.{CheckYourAnswersPage, JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.AllCorrectionPeriodsQuery

import scala.util.Try

case class CorrectPreviousReturnPage(numberOfExistingReturnPeriods: Int) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctPreviousReturn"

  override def route(waypoints: Waypoints): Call = controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {

    answers.get(CorrectPreviousReturnPage(numberOfExistingReturnPeriods)) match {
      case Some(true) => if (numberOfExistingReturnPeriods > 1) {
        CorrectionReturnYearPage(Index(0))
      } else {
        CorrectionReturnSinglePeriodPage(Index(0))
      }
      case Some(false) => CheckYourAnswersPage
      case _ => JourneyRecoveryPage
    }
  }

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] = {

    val changedFromYesToNo:Option[Boolean] = for {
      currentAnswers <- value
      previousAnswers <- userAnswers.get(AllCorrectionPeriodsQuery).map(_.nonEmpty)
    } yield {
      previousAnswers && !currentAnswers
    }

    if(changedFromYesToNo.getOrElse(false)) {
      userAnswers.remove(AllCorrectionPeriodsQuery)
    } else {
      Try(userAnswers)
    }
  }
}
