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

package controllers

import models.requests.DataRequest
import models.{Country, Index, VatRateFromCountry}
import pages.{JourneyRecoveryPage, SoldToCountryPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.VatRatesFromCountryQuery
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait SalesFromCountryBaseController {

  protected def getCountryAndVatRate(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index)
                                    (block: (Country, VatRateFromCountry) => Result)
                                    (implicit request: DataRequest[AnyContent]): Result = {
    (for {
      country <- request.userAnswers.get(SoldToCountryPage(countryIndex))
      vatRate <- request.userAnswers.get(VatRatesFromCountryQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)))
  }


  protected def getCountryAndVatRateAsync(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index)
                                         (block: (Country, VatRateFromCountry) => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] =

    (for {
      country <- request.userAnswers.get(SoldToCountryPage(countryIndex))
      vatRate <- request.userAnswers.get(VatRatesFromCountryQuery(countryIndex, vatRateIndex))
    } yield block(country, vatRate))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
}
