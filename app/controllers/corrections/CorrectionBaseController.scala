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

package controllers.corrections

import controllers.JourneyRecoverySyntax._
import models.{Country, Index, Period}
import models.requests.DataRequest
import pages.{JourneyRecoveryPage, SoldToCountryPage, Waypoints}
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.{CorrectionPeriodQuery, DeriveNumberOfCorrections}
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait CorrectionBaseController {


  protected def getNumberOfCorrections(periodIndex: Index)
                                      (block: (Int, Period) => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =

    (for {
      numberOfCorrections <- request.userAnswers.get(DeriveNumberOfCorrections(periodIndex))
      correctionPeriod <- request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
    } yield block(numberOfCorrections, correctionPeriod))
      .orRecoverJourney

  protected def getNumberOfCorrectionsAsync(periodIndex: Index)
                                           (block: (Int, Period) => Future[Result])
                                           (implicit request: DataRequest[AnyContent]): Future[Result] =

    (for {
      numberOfCorrections <- request.userAnswers.get(DeriveNumberOfCorrections(periodIndex))
      correctionPeriod <- request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
    } yield block(numberOfCorrections, correctionPeriod))
      .orRecoverJourney

  protected def getCountry(waypoints: Waypoints, periodIndex: Index, countryIndex: Index)
                          (block: Country => Future[Result])
                          (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CorrectionCountryPage(periodIndex, countryIndex))
      .map(block(_))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)

  protected def getCorrectionReturnPeriod(waypoints: Waypoints, periodIndex: Index)
                                         (block: Period => Future[Result])
                                         (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(CorrectionPeriodQuery(periodIndex))
      .map(_.correctionReturnPeriod)
      .map(block(_))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints))).toFuture
}
