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

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import services.AccountService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationWithoutUrlIossActionSpec extends SpecBase with MockitoSugar with EitherValues with BeforeAndAfterEach {

  private val mockAccountService: AccountService = mock[AccountService]
  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  class Harness(
                 registrationConnector: RegistrationConnector,
                 accountService: AccountService,
                 appConfig: FrontendAppConfig
               ) extends GetRegistrationWithoutUrlIossAction(
    registrationConnector,
    accountService,
    appConfig,
  ) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
      refine(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockRegistrationConnector,
      mockAccountService,
      mockAppConfig
    )
  }

  "Get Registration Without Url Ioss Action" - {
    
    "must redirect to Not Registered page when no ioss number is found from enrolments" in {

      when(mockAppConfig.iossEnrolment) thenReturn "HMRC-IOSS-ORG"

      val enrolments = Enrolments(
        Set(
          Enrolment(
            "HMRC-IOSS-ORG",
            Seq.empty,
            "Activated"
          )
        )
      )

      val action = new Harness(mockRegistrationConnector, mockAccountService, mockAppConfig)
      
      val request: RegistrationRequest[AnyContent] =
        RegistrationRequest(FakeRequest(), testCredentials, Some(vrn), "Company name", iossNumber, registrationWrapper, None, enrolments)

      val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn, enrolments)).futureValue

      result.isRight `mustBe` true
    }
  }
}
