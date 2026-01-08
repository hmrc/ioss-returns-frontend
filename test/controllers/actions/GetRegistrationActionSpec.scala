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
import config.Constants.ukCountryCodeAreaPrefix
import config.FrontendAppConfig
import connectors.{IntermediaryRegistrationConnector, RegistrationConnector}
import models.RegistrationWrapper
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.IntermediarySelectedIossNumberRepository
import services.AccountService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationActionSpec extends SpecBase with MockitoSugar with EitherValues {

  class Harness(
                 accountService: AccountService,
                 intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                 registrationConnector: RegistrationConnector,
                 appConfig: FrontendAppConfig,
                 intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository
               ) extends GetRegistrationAction(
    accountService,
    intermediaryRegistrationConnector,
    registrationConnector,
    appConfig,
    None,
    intermediarySelectedIossNumberRepository
  ) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
      refine(request)
  }

  "Get Registration Action" - {

    "and a registration can be retrieved from the backend" - {

      "must save the registration to the sessionRepository and return Right" in {

        val arbitrayVatInfo = arbitraryVatInfo.arbitrary.sample.value
        val ukBasedDesAddress = arbitrayVatInfo.desAddress.copy(countryCode = ukCountryCodeAreaPrefix)
        val ukBasedVatInfo = arbitrayVatInfo.copy(desAddress = ukBasedDesAddress)

        val registrationWrapper = Arbitrary.arbitrary[RegistrationWrapper].sample.value.copy(
          vatInfo = Some(ukBasedVatInfo))

        val request = FakeRequest()
        val accountService = mock[AccountService]
        val intermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
        val registrationConnector = mock[RegistrationConnector]
        val appConfig = mock[FrontendAppConfig]
        val intermediarySelectedIossNumberRepository = mock[IntermediarySelectedIossNumberRepository]

        when(appConfig.iossEnrolment).thenReturn("HMRC-IOSS-ORG")

        val enrolments = Enrolments(
          Set(
            Enrolment(
              "HMRC-IOSS-ORG",
              Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
              "Activated"
            )
          )
        )

        when(registrationConnector.get(any())(any())) thenReturn registrationWrapper.toFuture

        val action = new Harness(accountService, intermediaryRegistrationConnector, registrationConnector, appConfig, intermediarySelectedIossNumberRepository)

        val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn, enrolments)).futureValue

        result.isRight mustEqual true
      }
    }
  }
}
