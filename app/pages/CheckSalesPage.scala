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
import models.{Index, UserAnswers}
import play.api.libs.json.{JsObject, JsPath}
import play.api.mvc.Call
import queries.{Derivable, DeriveNumberOfSales}

object CheckSalesPage {

  val normalModeUrlFragment: String = "check-sales"
  val checkModeUrlFragment: String = "change-check-sales"
}

final case class CheckSalesPage(override val index: Option[Index] = None) extends AddItemPage(index) with QuestionPage[Boolean] {

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

  // TODO -> Need to check VatRatesFromCountryPage for how many of available vat rates for country have been checked, then remaining ones should be available to be added here
  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(this).map {
      case true =>
        index
          .map { i =>
            if (i.position + 1 < 5) { // TODO -> Replace with query checking how many checked VAT rates
              SalesToCountryPage(Index(i.position + 1))
            } else {
              SoldToCountryListPage(Some(i))
            }
          }.getOrElse {
          answers
            .get(deriveNumberOfItems)
            .map(n => SalesToCountryPage(Index(n)))
            .orRecover
        }
      case false => SoldToCountryListPage(index)
    }.orRecover

  override def deriveNumberOfItems: Derivable[Seq[JsObject], Int] = DeriveNumberOfSales // TODO -> Replace with query checking max vat rats for country
}