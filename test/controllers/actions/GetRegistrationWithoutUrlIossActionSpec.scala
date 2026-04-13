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
import connectors.RegistrationConnector
import controllers.routes
import models.etmp.intermediary.EtmpCustomerIdentificationLegacy
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import services.AccountService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationWithoutUrlIossActionSpec extends SpecBase with MockitoSugar with EitherValues with BeforeAndAfterEach {

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockAccountService: AccountService = mock[AccountService]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockAppConfig.iossEnrolment) thenReturn "HMRC-IOSS-ORG"

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

    "must redirect to Not Registered page when no IOSS enrolment exists" in {

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

      val request: IdentifierRequest[AnyContent] = IdentifierRequest[AnyContent](FakeRequest(), testCredentials, vrn, enrolments)

      val result = action.callRefine(request).futureValue

      result `mustBe` Left(Redirect(routes.NotRegisteredController.onPageLoad()))
      verifyNoInteractions(mockRegistrationConnector)
      verifyNoInteractions(mockAccountService)
    }

    "when a single IOSS enrolment exists" - {

      "must return Right(RegistrationRequest)" in {

        val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
          vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
            desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
              countryCode = ukCountryCodeAreaPrefix
            ),
            organisationName = Some("organisation name")
          )),
          registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
            customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
            otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
              tradingName = Some("trading name")
            ))
          )
        )

        val enrolments = Enrolments(
          Set(
            Enrolment(
              key = mockAppConfig.iossEnrolment,
              identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
              state = "Activated"
            )
          )
        )

        when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

        val action = new Harness(mockRegistrationConnector, mockAccountService, mockAppConfig)

        val request: IdentifierRequest[AnyContent] = IdentifierRequest[AnyContent](FakeRequest(), testCredentials, vrn, enrolments)

        val result = action.callRefine(request).futureValue

        result `mustBe` Right(
          RegistrationRequest(
            request = request.request,
            credentials = request.credentials,
            vrn = registrationWrapper.maybeVrn,
            companyName = registrationWrapper.getCompanyName(),
            iossNumber = iossNumber,
            registrationWrapper = registrationWrapper,
            intermediaryNumber = None,
            enrolments = request.enrolments
          )
        )

        verify(mockRegistrationConnector, times(1)).get()(any())
        verifyNoInteractions(mockAccountService)
      }

      "and an Intermediary exists" - {

        "must return Right(RegistrationRequest)" in {

          val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
            vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
                countryCode = ukCountryCodeAreaPrefix
              ),
              organisationName = Some("organisation name")
            )),
            registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
              customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
              otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
                tradingName = Some("trading name")
              ))
            )
          )

          val enrolments = Enrolments(
            Set(
              Enrolment(
                key = mockAppConfig.iossEnrolment,
                identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
                state = "Activated"
              ),
              Enrolment(
                key = mockAppConfig.intermediaryEnrolment,
                identifiers = Seq(EnrolmentIdentifier("IntNumber", intermediaryNumber)),
                state = "Activated"
              )
            )
          )

          when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

          val action = new Harness(mockRegistrationConnector, mockAccountService, mockAppConfig)

          val request: IdentifierRequest[AnyContent] = IdentifierRequest[AnyContent](FakeRequest(), testCredentials, vrn, enrolments)

          val result = action.callRefine(request).futureValue

          result `mustBe` Right(
            RegistrationRequest(
              request = request.request,
              credentials = request.credentials,
              vrn = registrationWrapper.maybeVrn,
              companyName = registrationWrapper.getCompanyName(),
              iossNumber = iossNumber,
              registrationWrapper = registrationWrapper,
              intermediaryNumber = Some(intermediaryNumber),
              enrolments = request.enrolments
            )
          )

          verify(mockRegistrationConnector, times(1)).get()(any())
          verifyNoInteractions(mockAccountService)
        }
      }
    }

    "when multiple IOSS enrolments exist" - {

      "must retrieve the latest enrolment and return Right(RegistrationRequest)" in {

        val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
          vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
            desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
              countryCode = ukCountryCodeAreaPrefix
            ),
            organisationName = Some("organisation name")
          )),
          registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
            customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
            otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
              tradingName = Some("trading name")
            ))
          )
        )

        val enrolments = Enrolments(
          Set(
            Enrolment(
              key = mockAppConfig.iossEnrolment,
              identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
              state = "Activated"
            ),
            Enrolment(
              key = mockAppConfig.iossEnrolment,
              identifiers = Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234568")),
              state = "Activated"
            )
          )
        )

        when(mockAccountService.getLatestAccount()(any())) thenReturn iossNumber.toFuture
        when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

        val action = new Harness(mockRegistrationConnector, mockAccountService, mockAppConfig)

        val request: IdentifierRequest[AnyContent] = IdentifierRequest[AnyContent](FakeRequest(), testCredentials, vrn, enrolments)

        val result = action.callRefine(request).futureValue

        result `mustBe` Right(
          RegistrationRequest(
            request = request.request,
            credentials = request.credentials,
            vrn = registrationWrapper.maybeVrn,
            companyName = registrationWrapper.getCompanyName(),
            iossNumber = iossNumber,
            registrationWrapper = registrationWrapper,
            intermediaryNumber = None,
            enrolments = request.enrolments
          )
        )

        verify(mockRegistrationConnector, times(1)).get()(any())
        verify(mockAccountService, times(1)).getLatestAccount()(any())
      }

      "and an Intermediary exists" - {

        "must retrieve the latest enrolment and return Right(RegistrationRequest)" in {

          val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
            vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
                countryCode = ukCountryCodeAreaPrefix
              ),
              organisationName = Some("organisation name")
            )),
            registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
              customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
              otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
                tradingName = Some("trading name")
              ))
            )
          )

          val enrolments = Enrolments(
            Set(
              Enrolment(
                key = mockAppConfig.iossEnrolment,
                identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
                state = "Activated"
              ),
              Enrolment(
                key = mockAppConfig.iossEnrolment,
                identifiers = Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234568")),
                state = "Activated"
              ),
              Enrolment(
                key = mockAppConfig.intermediaryEnrolment,
                identifiers = Seq(EnrolmentIdentifier("IntNumber", intermediaryNumber)),
                state = "Activated"
              )
            )
          )

          when(mockAccountService.getLatestAccount()(any())) thenReturn iossNumber.toFuture
          when(mockRegistrationConnector.get()(any())) thenReturn registrationWrapper.toFuture

          val action = new Harness(mockRegistrationConnector, mockAccountService, mockAppConfig)

          val request: IdentifierRequest[AnyContent] = IdentifierRequest[AnyContent](FakeRequest(), testCredentials, vrn, enrolments)

          val result = action.callRefine(request).futureValue

          result `mustBe` Right(
            RegistrationRequest(
              request = request.request,
              credentials = request.credentials,
              vrn = registrationWrapper.maybeVrn,
              companyName = registrationWrapper.getCompanyName(),
              iossNumber = iossNumber,
              registrationWrapper = registrationWrapper,
              intermediaryNumber = Some(intermediaryNumber),
              enrolments = request.enrolments
            )
          )

          verify(mockRegistrationConnector, times(1)).get()(any())
          verify(mockAccountService, times(1)).getLatestAccount()(any())
        }
      }
    }
  }
}
