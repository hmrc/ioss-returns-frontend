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

import connectors.VatReturnConnector
import logging.Logging
import models.etmp.{EtmpObligations, EtmpObligationsFulfilmentStatus}
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckSubmittedReturnsFilterImpl(
                                     connector: VatReturnConnector
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionFilter[DataRequest] with Logging {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    connector.getObligations(request.iossNumber)
      .map(obligations)
      .recover {
        case e: Exception =>
          logger.error(s"Error occurred while getting obligations ${e.getMessage}", e)
          None
      }
  }

    private def obligations(etmpObligations: EtmpObligations): Option[Result] = {
      etmpObligations.obligations.flatMap(_.obligationDetails).minByOption(_.periodKey) match {
        case Some(etmpObligation) if etmpObligation.status == EtmpObligationsFulfilmentStatus.Fulfilled =>
          None
        case _ =>
          Some(Redirect(controllers.routes.CheckYourAnswersController.onPageLoad()))
      }
    }

}

class CheckSubmittedReturnsFilterProvider @Inject()(
                                                   connector: VatReturnConnector
                                                   )(implicit ec:ExecutionContext) {

  def apply(): CheckSubmittedReturnsFilterImpl =
    new CheckSubmittedReturnsFilterImpl(connector)
}