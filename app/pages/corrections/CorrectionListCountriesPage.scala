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

import models.{Country, Index, UserAnswers}
import pages.{AddItemPage, JourneyRecoveryPage, Page, QuestionPage, Waypoints}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfCorrections}

object CorrectionListCountriesPage {
  val normalModeUrlFragment: String = "add-correction-list-countries"
  val checkModeUrlFragment: String = "change-add-correction-list-countries"
}

final case class CorrectionListCountriesPage(periodIndex: Option[Index] = None) extends AddItemPage(periodIndex) with QuestionPage[Boolean] {

  override def isTheSamePage(other: Page): Boolean = other match {
    case _: CorrectionListCountriesPage => true
    case _ => false
  }

  override val normalModeUrlFragment: String = CorrectionListCountriesPage.normalModeUrlFragment
  override val checkModeUrlFragment: String = CorrectionListCountriesPage.checkModeUrlFragment

  override def path: JsPath = JsPath \ toString

  override def toString: String = "correctionListCountries"

  override def route(waypoints: Waypoints): Call = controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, Index(0))

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        index.map { i =>
          if (i.position +1 < Country.euCountriesWithNI.size) {
            CorrectionCountryPage(Index(0), Index(i.position + 1))
          } else {
            JourneyRecoveryPage
          }
        }
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => CorrectionCountryPage(Index(0), Index(n)))
              .orRecover
          }
      case false =>
        JourneyRecoveryPage
    }.orRecover

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfCorrections(Index(0))
}
