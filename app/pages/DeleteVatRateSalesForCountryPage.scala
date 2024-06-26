/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.mvc.Call
import queries.DeriveNumberOfVatRatesFromCountry

case class DeleteVatRateSalesForCountryPage(countryIndex: Index, vatRateIndex: Index) extends Page {

  override def route(waypoints: Waypoints): Call =
    routes.DeleteVatRateSalesForCountryController.onPageLoad(waypoints, countryIndex, vatRateIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    answers.get(DeriveNumberOfVatRatesFromCountry(countryIndex)) match {
      case Some(n) if n > 0 =>
        CheckSalesPage(countryIndex)
      case _ =>
        VatRatesFromCountryPage(countryIndex, vatRateIndex)
    }

  override protected def nextPageCheckMode(waypoints: NonEmptyWaypoints, answers: UserAnswers): Page =
    answers.get(DeriveNumberOfVatRatesFromCountry(countryIndex)) match {
      case Some(n) if n > 0 =>
        CheckSalesPage(countryIndex)
      case _ =>
        VatRatesFromCountryPage(countryIndex, vatRateIndex)
    }
}
