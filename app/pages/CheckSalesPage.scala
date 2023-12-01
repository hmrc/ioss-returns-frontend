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
import models.{Index, UserAnswers}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, RemainingVatRatesFromCountryQuery}

object CheckSalesPage {

  val normalModeUrlFragment: String = "check-sales"
  val checkModeUrlFragment: String = "change-check-sales"
}

final case class CheckSalesPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] with Logging {

  override def isTheSamePage(other: Page): Boolean = other match {
    case p: CheckSalesPage => p.index == this.index
    case _ => false
  }

  override def path: JsPath = JsPath \ toString

  override def toString: String = "check-sales"

  override def route(waypoints: Waypoints): Call =
    routes.CheckSalesController.onPageLoad(waypoints, index.getOrElse(Index(0)))

  override val normalModeUrlFragment: String = CheckSalesPage.normalModeUrlFragment
  override val checkModeUrlFragment: String = CheckSalesPage.checkModeUrlFragment

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this) match {
      case Some(true) =>
        index.flatMap { countryIndex =>
          determinePageRedirect(answers, countryIndex)
          }.orRecover
      case Some(false) =>
        SoldToCountryListPage(index)
      case _ =>
        JourneyRecoveryPage
    }

  private def determinePageRedirect(answers: UserAnswers, countryIndex: Index): Option[Page] = {
    answers.get(RemainingVatRatesFromCountryQuery(countryIndex)).flatMap {
      case vatRatesFromCountry if vatRatesFromCountry.size == 1 =>
        Some(RemainingVatRateFromCountryPage(countryIndex, Index(vatRatesFromCountry.size))) // TODO -> Check vatRateIndex size or size -1
      case vatRatesFromCountry if vatRatesFromCountry.size > 1 =>
        Some(VatRatesFromCountryPage(countryIndex))
      case vatRatesFromCountry if vatRatesFromCountry.isEmpty =>
        val exception = new IllegalStateException("VAT rate missing")
        logger.error(exception.getMessage, exception)
        throw exception
      case _ =>
        Some(JourneyRecoveryPage)
    }
  }

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = ???
}