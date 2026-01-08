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

package pages.corrections

import models.{Index, UserAnswers}
import pages.{JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatPayableForCountryPage(periodIndex: Index, countryIndex: Index) extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "vatPayableForCountry"

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    nextPageNormalMode(waypoints, answers)
  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(VatPayableForCountryPage(periodIndex, countryIndex)) match {
      case Some(true) => CheckVatPayableAmountPage(periodIndex, countryIndex)
      case Some(false) => VatAmountCorrectionCountryPage(periodIndex, countryIndex)

      case _ => JourneyRecoveryPage
    }

  override def route(waypoints: Waypoints): Call =
    controllers.corrections.routes.VatPayableForCountryController.onPageLoad(waypoints, periodIndex, countryIndex)
}
