/*
 * Copyright 2026 HM Revenue & Customs
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
import models.{Country, Index}
import pages.{JourneyRecoveryPage, SoldToCountryPage, Waypoints}
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import utils.FutureSyntax.FutureOps

import scala.concurrent.Future

trait GetCountry {

  protected def getCountry(waypoints: Waypoints, index: Index)
                          (block: Country => Future[Result])
                          (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers
      .get(SoldToCountryPage(index))
      .map(block(_))
      .getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
}
