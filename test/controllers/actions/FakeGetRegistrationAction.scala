/*
 * Copyright 2024 HM Revenue & Customs
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
import connectors.{IntermediaryRegistrationConnector, RegistrationConnector}
import models.RegistrationWrapper
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import repositories.IntermediarySelectedIossNumberRepository
import services.AccountService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import utils.FutureSyntax.*

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class FakeGetRegistrationAction(registration: RegistrationWrapper)
  extends GetRegistrationAction(
    mock[AccountService],
    mock[IntermediaryRegistrationConnector],
    mock[RegistrationConnector],
    mock[FrontendAppConfig],
    None,
    mock[IntermediarySelectedIossNumberRepository]
  ) {

  private val iossEnrolmentKey = "HMRC-IOSS-ORG"
  private val enrolments: Enrolments = Enrolments(Set(Enrolment(iossEnrolmentKey, Seq.empty, "test", None)))

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
    Right(RegistrationRequest(request.request, request.credentials, Some(request.vrn), "Company Name", "IM9001234567", registration, None, enrolments)).toFuture
}

class FakeGetRegistrationActionProvider(registrationWrapper: RegistrationWrapper)
extends GetRegistrationActionProvider(
  mock[AccountService],
  mock[IntermediaryRegistrationConnector],
  mock[RegistrationConnector],
  mock[IntermediarySelectedIossNumberRepository],
  mock[FrontendAppConfig]
)(ExecutionContext.Implicits.global) {

  override def apply(maybeIossNumber: Option[String] = None): GetRegistrationAction = new FakeGetRegistrationAction(registrationWrapper)
}
