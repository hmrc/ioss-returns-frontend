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
import models.RegistrationWrapper
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import services.AccountService
import testUtils.RegistrationData.{iossEnrolmentKey, iossNumber}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

// TODO -> Need this????
class FakeGetRegistrationWithoutUrlIossAction(
                                               registrationWrapper: RegistrationWrapper,
                                               enrolments: Option[Enrolments] = None,
                                               maybeIntermediaryNumber: Option[String] = None
                                             )
  extends GetRegistrationWithoutUrlIossAction(
    mock[RegistrationConnector],
    mock[AccountService],
    mock[FrontendAppConfig]
  )(ExecutionContext.Implicits.global) {

  private val iossEnrolment: Enrolments =
    Enrolments(Set(Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)), "test", None)))

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] = {
    Right(
      RegistrationRequest(
        request = request.request,
        credentials = request.credentials,
        vrn = Some(request.vrn),
        companyName = registrationWrapper.getCompanyName(),
        iossNumber = getIossNumberFromEnrolments,
        registrationWrapper = registrationWrapper,
        intermediaryNumber = maybeIntermediaryNumber,
        enrolments = enrolments.getOrElse(iossEnrolment)
      )
    ).toFuture
  }

  private def getIossNumberFromEnrolments: String = {
    enrolments match {
      case Some(e) =>
        e.enrolments.head.identifiers.head.value
      case _ =>
        "IM9001234567"
    }
  }
}
