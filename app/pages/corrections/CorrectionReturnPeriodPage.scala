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
import pages.{JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath

import play.api.mvc.Call




case class CorrectionReturnPeriodPage(index: Index) extends QuestionPage[String] {

  override def path: JsPath = JsPath \ "corrections" \ index.position \ toString

  override def toString: String = "correctionReturnPeriod"

  override def route(waypoints: Waypoints): Call =
    controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(CorrectionReturnPeriodPage(index)) match {
      case Some(_) => CorrectionCountryPage(Index(0), index)
      case _ => JourneyRecoveryPage
    }
}
