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

package controllers.actions

import config.FrontendAppConfig
import connectors.IntermediaryRegistrationConnector
import models.requests.RegistrationRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckPartOfVatGroupWithFixedEstablishmentsFilterImpl(
                                                            intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                                            frontendAppConfig: FrontendAppConfig
                                                          )(implicit protected val executionContext: ExecutionContext)
  extends ActionFilter[RegistrationRequest] {

  override protected def filter[A](request: RegistrationRequest[A]): Future[Option[Result]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)

    request.intermediaryNumber match {
      case Some(intermediaryNumber) =>
        checkIsClientOfIntermediary(intermediaryNumber, request.iossNumber).flatMap {
          case true =>
            None.toFuture

          case _ if checkPartOfVatGroupWithFE(request) =>
            Some(Redirect(frontendAppConfig.amendRegistrationUrl)).toFuture

          case _ =>
            None.toFuture
        }

      case _ if checkPartOfVatGroupWithFE(request) =>
        Some(Redirect(frontendAppConfig.amendRegistrationUrl)).toFuture

      case _ =>
        None.toFuture
    }
  }

  private def checkIsClientOfIntermediary(intermediaryNumber: String, iossNumber: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    intermediaryRegistrationConnector.get(intermediaryNumber).map { registrationWrapper =>
      registrationWrapper
        .etmpDisplayRegistration.clientDetails.exists(_.clientIossID == iossNumber)
    }
  }

  private def checkPartOfVatGroupWithFE(registrationRequest: RegistrationRequest[_]): Boolean = {
    val partOfVatGroup: Boolean = registrationRequest.registrationWrapper.vatInfo.exists(_.partOfVatGroup)
    val hasFixedEstablishments: Boolean = registrationRequest.registrationWrapper.registration.schemeDetails.euRegistrationDetails.nonEmpty

    hasFixedEstablishments && partOfVatGroup
  }
}

class CheckPartOfVatGroupWithFixedEstablishmentsFilter @Inject()(
                                                                  intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                                                  frontendAppConfig: FrontendAppConfig
                                                                )(implicit executionContext: ExecutionContext) {
  def apply(): CheckPartOfVatGroupWithFixedEstablishmentsFilterImpl = {
    new CheckPartOfVatGroupWithFixedEstablishmentsFilterImpl(intermediaryRegistrationConnector, frontendAppConfig)
  }
}
