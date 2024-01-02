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

package pages

import controllers.routes
import logging.Logging
import models.{CheckMode, Index, NormalMode, UserAnswers}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfVatRatesFromCountry, RemainingVatRatesFromCountryQuery}


final case class CheckSalesPage(countryIndex: Index, vatRateIndex: Option[Index] = None)
  extends AddItemPage(vatRateIndex) with QuestionPage[Boolean] with Logging {

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckSalesPage => p.countryIndex == this.countryIndex
    case _ => false
  }

  override def path: JsPath = JsPath \ toString

  override def toString: String = "checkSales"

  override def route(waypoints: Waypoints): Call =
    routes.CheckSalesController.onPageLoad(waypoints, countryIndex)

  override val normalModeUrlFragment: String = CheckSalesPage.normalModeUrlFragment(countryIndex)
  override val checkModeUrlFragment: String = CheckSalesPage.checkModeUrlFragment(countryIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    println("check sales nextPageNormalMode")
    answers.get(this).map {
      case true => {
        println("check sales nextPageNormalMode true")
        vatRateIndex
          .map(i => determinePageRedirect(answers, countryIndex, Index(i.position + 1)))
          .getOrElse {
            answers
              .get(deriveNumberOfItems)
              .map(n => determinePageRedirect(answers, countryIndex, Index(n)))
              .orRecover
          }}
      case false => {
        println("check sales nextPageNormalMode false")
        SoldToCountryListPage(index)
      }
    }.orRecover
  }

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    nextPageNormalMode(waypoints, answers)


  private def determinePageRedirect(answers: UserAnswers, countryIndex: Index, vatRateIndex: Index): Page = {
    println("check sales determinePageRedirect")
    answers.get(RemainingVatRatesFromCountryQuery(countryIndex)).flatMap {
      case vatRatesFromCountry if vatRatesFromCountry.size == 1 =>
        Some(RemainingVatRateFromCountryPage(countryIndex, vatRateIndex))
      case vatRatesFromCountry if vatRatesFromCountry.size > 1 =>
        Some(VatRatesFromCountryPage(countryIndex, vatRateIndex))
      case vatRatesFromCountry if vatRatesFromCountry.isEmpty =>
        val exception = new IllegalStateException("VAT rate missing")
        logger.error(exception.getMessage, exception)
        throw exception
      case _ => Some(JourneyRecoveryPage)
    }.orRecover
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfVatRatesFromCountry(countryIndex)
}

object CheckSalesPage {

  def normalModeUrlFragment(countryIndex: Index): String = s"check-sales-${countryIndex.display}"

  def checkModeUrlFragment(countryIndex: Index): String = s"change-check-sales-${countryIndex.display}"

  def waypointFromString(s: String): Option[Waypoint] = {

    val normalModePattern = """check-sales-(\d{1,3})""".r.anchored
    val checkModePattern = """change-check-sales-(\d{1,3})""".r.anchored

    s match {
      case normalModePattern(indexDisplay) =>
        println ("==== normal ")
        Some(CheckSalesPage(Index(indexDisplay.toInt - 1), None).waypoint(NormalMode))

      case checkModePattern(indexDisplay) =>
        println ("==== check ")
        Some(CheckSalesPage(Index(indexDisplay.toInt - 1), None).waypoint(CheckMode))

      case _ =>
        None
    }
  }
}