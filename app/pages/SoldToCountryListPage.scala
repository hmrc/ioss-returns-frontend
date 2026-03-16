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

package pages

import controllers.routes
import models.{CheckMode, Index, NormalMode, UserAnswers}
import pages.corrections.CorrectPreviousReturnPage
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfSales}

object SoldToCountryListPage {

  def normalModeUrlFragment(iossNumber: String): String = s"$iossNumber-add-sales-country-list"

  def checkModeUrlFragment(iossNumber: String): String = s"$iossNumber-change-add-sales-country-list"

  def waypointFromString(s: String): Option[Waypoint] = {

    val normalModePattern = """((?i)IM\d{10})-add-sales-country-list""".r.anchored
    val checkModePattern = """((?i)IM\d{10})-change-add-sales-country-list""".r.anchored

    s match {
      case normalModePattern(iossNumber) =>
        Some(SoldToCountryListPage(iossNumber).waypoint(NormalMode))

      case checkModePattern(iossNumber) =>
        Some(SoldToCountryListPage(iossNumber).waypoint(CheckMode))

      case _ =>
        None
    }
  }
}

final case class SoldToCountryListPage(iossNumber: String, override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: SoldToCountryListPage => true
    case _ => false
  }

  override def path: JsPath = JsPath \ toString

  override def toString: String = "soldToCountryList"

  override def route(waypoints: Waypoints): Call =
    routes.SoldToCountryListController.onPageLoad(waypoints, iossNumber)
    
  override val normalModeUrlFragment: String = SoldToCountryListPage.normalModeUrlFragment(iossNumber)
  override val checkModeUrlFragment: String = SoldToCountryListPage.checkModeUrlFragment(iossNumber)

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    nextPageNormalMode(waypoints, answers)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(this).map {
      case true =>
        answers
          .get(deriveNumberOfItems)
          .map(n => SoldToCountryPage(answers.iossNumber, Index(n)))
          .orRecover
        
      case false =>
        if (answers.isDefined(CorrectPreviousReturnPage(0))) {
          CheckYourAnswersPage
        } else {
          CorrectPreviousReturnPage(0)
        }
    }.orRecover
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfSales
}
