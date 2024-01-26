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

package controllers.actions

import config.FrontendAppConfig
import controllers.routes
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, TransferringMSID, VoluntarilyLeaves}
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckExcludedTraderFilterImpl(frontendAppConfig: FrontendAppConfig
                                   )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[DataRequest] {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    if (frontendAppConfig.exclusionsEnabled) {
      request.registrationWrapper.registration.exclusions.lastOption match {
        case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason)
          && request.userAnswers.period.lastDay.isAfter(exclusion.effectiveDate) =>
          Future.successful(Some(Redirect(routes.ExcludedNotPermittedController.onPageLoad())))
        case _ =>
          Future.successful(None)
      }
    } else {
      Future.successful(None)
    }
    Future.successful(Some(Redirect(routes.ExcludedNotPermittedController.onPageLoad())))
  }
}

class CheckExcludedTraderFilter @Inject()(frontendAppConfig: FrontendAppConfig)
                                         (implicit ec: ExecutionContext) {

  def apply(): CheckExcludedTraderFilterImpl =
    new CheckExcludedTraderFilterImpl(frontendAppConfig)

}
