/*
 * Copyright 2023 HM Revenue & Customs
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
import queries.corrections.DeriveCompletedCorrectionPeriods

case object CorrectPreviousReturnPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctPreviousReturn"

  override def route(waypoints: Waypoints): Call = controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {

    val correctedPeriods: Int = answers.get(DeriveCompletedCorrectionPeriods).map(_.size).getOrElse(0)

    answers.get(CorrectPreviousReturnPage) match {
      case Some(true) if correctedPeriods > 0 => CheckYourAnswersPage
      case Some(true) => if (correctedPeriods > 1) { //todo uncorrectedPeriods when API is
        CorrectionReturnYearPage(Index(0))
      } else {
        CorrectionReturnYearPage(Index(0))
      }
      case Some(false) => CheckYourAnswersPage
      case _ => JourneyRecoveryPage
    }
  }
}
