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

import models.{CheckMode, Index, NormalMode, UserAnswers}
import pages.{Page, PageAndWaypoints, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call
import queries.DeriveNumberOfCorrectionPeriods

case class RemovePeriodCorrectionPage(periodIndex: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "removePeriodCorrection"

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(DeriveNumberOfCorrectionPeriods) match {
      case Some(numberOfPeriods) if numberOfPeriods > 0 => VatPeriodCorrectionsListPage(answers.period, false) //Todo: Addanother??
      case _ => CorrectPreviousReturnPage(0) //Todo?
    }

  override def route(waypoints: Waypoints): Call = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(waypoints, periodIndex)
}