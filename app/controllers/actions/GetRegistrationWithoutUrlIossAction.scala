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
import connectors.RegistrationConnector
import controllers.routes
import models.requests.{IdentifierRequest, RegistrationRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.AccountService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// TODO -> Test and Fake?
class GetRegistrationWithoutUrlIossAction @Inject()(
                                                     val registrationConnector: RegistrationConnector,
                                                     accountService: AccountService,
                                                     config: FrontendAppConfig
                                                   )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, RegistrationRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)

    (for {
      maybeIossNumberFromEnrolments <- findIossFromEnrolments(request.enrolments)
    } yield {
      maybeIossNumberFromEnrolments match {
        case Some(iossNumber) =>
          registrationConnector.get().map { registration =>
            Right(RegistrationRequest(
              request = request.request,
              credentials = request.credentials,
              vrn = Some(request.vrn),
              companyName = registration.getCompanyName(),
              iossNumber = iossNumber,
              registrationWrapper = registration,
              intermediaryNumber = None,
              enrolments = request.enrolments
            ))
          }

        case _ =>
          Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
      }
    }).flatten
  }


  private def findIossFromEnrolments(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val filteredIossNumbers = enrolments
      .enrolments
      .filter(_.key == config.iossEnrolment)
      .flatMap(_.identifiers.filter(_.key == "IOSSNumber").map(_.value))
      .toSeq

    filteredIossNumbers match {
      case firstEnrolment :: Nil => Some(firstEnrolment).toFuture
      case multipleEnrolments if multipleEnrolments.nonEmpty =>
        accountService.getLatestAccount().map(x => Some(x))
      case _ => None.toFuture
    }
  }
}
