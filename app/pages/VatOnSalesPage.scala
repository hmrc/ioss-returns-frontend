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
import models.{Index, UserAnswers, VatOnSales}
import pages.PageConstants.{salesAtVatRate, vatRates}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatOnSalesPage(countryIndex: Index, vatRateIndex: Index) extends QuestionPage[VatOnSales] {

  override def path: JsPath = JsPath \ PageConstants.sales \ countryIndex.position \ PageConstants.vatRates \ vatRateIndex.position \ salesAtVatRate \ toString

  override def toString: String = "vatOnSales"

  override def route(waypoints: Waypoints): Call = routes.VatOnSalesController.onPageLoad(waypoints, countryIndex, vatRateIndex)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    answers.get(VatRatesFromCountryPage(countryIndex, vatRateIndex)).map {
      rates =>
        println ("=== rate size: " + rates.size)
        println ("=== vat rate position: " + vatRateIndex.position)
        if (rates.size > vatRateIndex.position + 1) {
          println ("=== sales to country page: ")
          SalesToCountryPage(countryIndex, vatRateIndex + 1)
        } else {
          println ("==== check sales page")
          CheckSalesPage(countryIndex)
        }
    }.getOrElse(JourneyRecoveryPage)
  }
}
