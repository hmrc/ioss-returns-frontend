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

import models.{Country, Index, NormalMode, UserAnswers}
import pages.{AddItemPage, JourneyRecoveryPage, NonEmptyWaypoints, Page, QuestionPage, Waypoint, Waypoints}
import pages.RecoveryOps
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfCorrections}


final case class CorrectionListCountriesPage(periodIndex: Index, countryIndex: Option[Index] = None)
  extends AddItemPage(countryIndex) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: CorrectionListCountriesPage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = CorrectionListCountriesPage.normalModeUrlFragment(periodIndex)
  override val checkModeUrlFragment: String = CorrectionListCountriesPage.checkModeUrlFragment(periodIndex)

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctionListCountries"

  override def route(waypoints: Waypoints): Call =
    controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, periodIndex)

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    nextPageNormalMode(waypoints, answers)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        index.map { i =>
          if (i.position +1 < Country.euCountriesWithNI.size) {
            CorrectionCountryPage(periodIndex, Index(i.position + 1))
          } else {
            JourneyRecoveryPage
          }
        }
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => CorrectionCountryPage(periodIndex, Index(n)))
              .orRecover
          }
      case false =>
        VatPeriodCorrectionsListPage(answers.period, addAnother = true)
    }.orRecover

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfCorrections(periodIndex)
}

object CorrectionListCountriesPage {
  def normalModeUrlFragment(periodIndex: Index): String = s"add-correction-list-countries-${periodIndex.display}"
  def checkModeUrlFragment(periodIndex: Index): String = s"change-add-correction-list-countries-${periodIndex.display}"

  def waypointFromString(s: String): Option[Waypoint] = {

    val normalModePattern = """add-correction-list-countries-(\d{1,3})""".r.anchored
    val checkModePattern = """change-add-correction-list-countries-(\d{1,3})""".r.anchored

    s match {
      case normalModePattern(indexDisplay) =>
        Some(CorrectionListCountriesPage(Index(indexDisplay.toInt - 1), None).waypoint(NormalMode))
      case checkModePattern(indexDisplay) =>
        Some(CorrectionListCountriesPage(Index(indexDisplay.toInt - 1), None).waypoint(NormalMode))

      case _ =>
        None
    }
  }
}