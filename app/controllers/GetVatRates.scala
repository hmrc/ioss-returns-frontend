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

package controllers

import models.requests.DataRequest
import models.{Index, VatRateFromCountry}
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.{AllSalesByCountryQuery, SalesToCountryWithOptionalSales, VatRateFromCountryQuery}
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait GetVatRates {

  protected def getVatRateFromCountry(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index)
                                     (block: VatRateFromCountry => Future[Result])
                                     (implicit request: DataRequest[AnyContent]): Future[Result] = {
    request.userAnswers
      .get(VatRateFromCountryQuery(countryIndex, vatRateIndex))
      .map(block(_))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
  }

  protected def getAllVatRatesFromCountry(waypoints: Waypoints, countryIndex: Index)
                                         (block: SalesToCountryWithOptionalSales => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] = {
      request.userAnswers
        .get(AllSalesByCountryQuery(countryIndex))
        .map(block(_))
        .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
  }
}
