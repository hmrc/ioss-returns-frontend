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
import models.etmp.EtmpDisplayRegistration
import models.{IntermediarySelectedIossNumber, RegistrationWrapper}
import models.etmp.intermediary.{EtmpCustomerIdentification, EtmpCustomerIdentificationLegacy, IntermediaryRegistrationWrapper}
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary
import org.scalatest.{BeforeAndAfterEach, EitherValues}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import repositories.IntermediarySelectedIossNumberRepository
import services.AccountService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationActionSpec extends SpecBase with MockitoSugar with EitherValues with BeforeAndAfterEach {

  val mockAccountService: AccountService = mock[AccountService]
  val mockIntermediaryRegistrationConnector: IntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockIntermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository = mock[IntermediarySelectedIossNumberRepository]

  class Harness(
                 accountService: AccountService,
                 intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                 registrationConnector: RegistrationConnector,
                 appConfig: FrontendAppConfig,
                 requestedMaybeIossNumber: Option[String],
                 intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository
               ) extends GetRegistrationAction(
    accountService,
    intermediaryRegistrationConnector,
    registrationConnector,
    appConfig,
    requestedMaybeIossNumber,
    intermediarySelectedIossNumberRepository
  ) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
      refine(request)
  }

  def intermediaryRegistrationWithClients(iossNumber: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryEtmpIntermediaryDisplayRegistration.arbitrary.sample.value.copy(
        customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
        clientDetails = iossNumber.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
      )
    )
  }

  def enrolmentsWithIntermediaries(intermediaryNumbers: Seq[String]): Enrolments = {
    Enrolments(intermediaryNumbers.map { int =>
      Enrolment(
        key = "HMRC-IOSS-INT",
        identifiers = Seq(EnrolmentIdentifier("IntNumber", int)),
        state = "Activated"
      )
    }.toSet
    )
  }

  def expectedRegistrationRequest[A](
                                      request: IdentifierRequest[A],
                                      iossNumber: String,
                                      registration: RegistrationWrapper,
                                      intermediaryNumber: Option[String]
                                    ): RegistrationRequest[A] = {

    RegistrationRequest(
      request = request.request,
      credentials = request.credentials,
      vrn = registration.maybeVrn,
      companyName = "organisation name ",
      iossNumber = iossNumber,
      registrationWrapper = registration,
      intermediaryNumber = intermediaryNumber,
      enrolments = request.enrolments
    )
  }

  def enrolmentsWithSingleIoss(iossNumber: String, config: FrontendAppConfig): Enrolments = {
    Enrolments(
      Set(
        Enrolment(
          key = config.iossEnrolment,
          identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
          state = "Activated"
        )
      )
    )
  }

  def enrolmentsWithMultipleIoss(iossNumbers: Seq[String], config: FrontendAppConfig): Enrolments = {
    Enrolments(
      iossNumbers.map { iossNumber =>
        Enrolment(
          key = config.iossEnrolment,
          identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
          state = "Activated"
        )
      }.toSet
    )
  }

  def enrolmentsWithIossAndIntermediary(iossNumber: String, intermediaryNumber: String, config: FrontendAppConfig): Enrolments = {
    Enrolments(
      Set(
        Enrolment(
          key = config.iossEnrolment,
          identifiers = Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)),
          state = "Activated"
        ),
        Enrolment(
          key = config.intermediaryEnrolment,
          identifiers = Seq(EnrolmentIdentifier("IntNumber", intermediaryNumber)),
          state = "Activated"
        )
      )
    )
  }

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockAccountService,
      mockIntermediaryRegistrationConnector,
      mockRegistrationConnector,
      mockAppConfig,
      mockIntermediarySelectedIossNumberRepository
    )
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

        val action = new Harness(accountService, intermediaryRegistrationConnector, registrationConnector, appConfig, None, intermediarySelectedIossNumberRepository)

        val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn, enrolments)).futureValue

        result.isRight mustEqual true
      }
    }

    "must return a Registration Request" - {

      "when exactly one IOSS enrolment exists" in {

        when(mockAppConfig.iossEnrolment) thenReturn "HMRC-IOSS-ORG"

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

        when(mockRegistrationConnector.get(any())(any())) thenReturn registrationWrapper.toFuture

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolmentsWithSingleIoss(iossNumber, mockAppConfig)
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          None,
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Right(expectedRegistrationRequest(
          request,
          iossNumber,
          registrationWrapper,
          None
        ))
      }

      "when multiple IOSS enrolments exist, use the latest account" in {

        val currentIoss = iossNumber
        val previousIoss = "IM9001234568"

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

        when(mockAppConfig.iossEnrolment) thenReturn "HMRC-IOSS-ORG"
        when(mockAccountService.getLatestAccount()(any())) thenReturn currentIoss.toFuture
        when(mockRegistrationConnector.get(any())(any())) thenReturn registrationWrapper.toFuture

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolmentsWithMultipleIoss(Seq(currentIoss, previousIoss), mockAppConfig)
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          requestedMaybeIossNumber = None,
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Right(expectedRegistrationRequest(
          request,
          iossNumber,
          registrationWrapper,
          None
        ))
      }

      "when an IOSS enrolment and intermediary enrolment exists" - {
        "and no requestedMaybeIossNumber is present" in {
          when(mockAppConfig.iossEnrolment) thenReturn "HMRC-IOSS-ORG"
          when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"

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

          val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(
            userId = userAnswersId,
            intermediaryNumber,
            iossNumber
          )

          when(mockRegistrationConnector.get(any())(any())) thenReturn registrationWrapper.toFuture
          when(mockIntermediarySelectedIossNumberRepository.get(any())) thenReturn Some(intermediarySelectedIossNumber).toFuture
          when(mockIntermediarySelectedIossNumberRepository.keepAlive(any())) thenReturn true.toFuture
          when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
            intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture

          val request = IdentifierRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            enrolmentsWithIossAndIntermediary(iossNumber, intermediaryNumber, mockAppConfig)
          )

          val action = new Harness(
            mockAccountService,
            mockIntermediaryRegistrationConnector,
            mockRegistrationConnector,
            mockAppConfig,
            None,
            mockIntermediarySelectedIossNumberRepository
          )

          val result = action.callRefine(request).futureValue

          result mustBe Right(expectedRegistrationRequest(
            request,
            iossNumber,
            registrationWrapper,
            Some(intermediaryNumber)
          ))
        }
      }

      "when an IOSS enrolment does not exist" - {

        "and both intermediary and requestedMaybeIossNumber are present" in {

          val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
            vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
                countryCode = ukCountryCodeAreaPrefix
              ),
              organisationName = Some("organisation name")
            )),
            registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
              otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
                tradingName = Some("trading name")
              ))
            )
          )

          val request = IdentifierRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            enrolmentsWithIntermediaries(Seq(intermediaryNumber))
          )

          when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"
          when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
            intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture
          when(mockRegistrationConnector.get(any())(any())) thenReturn
            registrationWrapper.toFuture

          val action = new Harness(
            mockAccountService,
            mockIntermediaryRegistrationConnector,
            mockRegistrationConnector,
            mockAppConfig,
            requestedMaybeIossNumber = Some(iossNumber),
            mockIntermediarySelectedIossNumberRepository
          )

          val result = action.callRefine(request).futureValue

          result mustBe Right(
            expectedRegistrationRequest(request, iossNumber, registrationWrapper, Some(intermediaryNumber))
          )
        }

        "when only intermediary is present and IOSS is retrieved from selected repository" in {

          val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
            vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
                countryCode = ukCountryCodeAreaPrefix
              ),
              organisationName = Some("organisation name")
            )),
            registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
              otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
                tradingName = Some("trading name")
              ))
            )
          )

          val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(
            userId = userAnswersId,
            intermediaryNumber,
            iossNumber
          )
          val request = IdentifierRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            enrolmentsWithIntermediaries(Seq(intermediaryNumber))
          )

          when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"
          when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
            intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture
          when(mockIntermediarySelectedIossNumberRepository.get(any())) thenReturn Some(intermediarySelectedIossNumber).toFuture
          when(mockIntermediarySelectedIossNumberRepository.keepAlive(any())) thenReturn true.toFuture
          when(mockRegistrationConnector.get(any())(any())) thenReturn
            registrationWrapper.toFuture

          val action = new Harness(
            mockAccountService,
            mockIntermediaryRegistrationConnector,
            mockRegistrationConnector,
            mockAppConfig,
            requestedMaybeIossNumber = None,
            mockIntermediarySelectedIossNumberRepository
          )

          val result = action.callRefine(request).futureValue

          result mustBe Right(
            expectedRegistrationRequest(request, iossNumber, registrationWrapper, Some(intermediaryNumber))
          )
        }

        "and previous intermediary enrolment has access to requestedMaybeIossNumber" in {

          val registrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value.copy(
            vatInfo = Some(arbitraryVatInfo.arbitrary.sample.value.copy(
              desAddress = arbitraryDesAddress.arbitrary.sample.value.copy(
                countryCode = ukCountryCodeAreaPrefix
              ),
              organisationName = Some("organisation name")
            )),
            registration = arbitraryEtmpDisplayRegistrationLegacy.arbitrary.sample.value.copy(
              otherAddress = Some(arbitraryEtmpOtherAddress.arbitrary.sample.value.copy(
                tradingName = Some("trading name")
              ))
            )
          )

          val currentIntermediary = intermediaryNumber
          val previousIntermediary = "IN9007654322"

          when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"
          when(mockIntermediaryRegistrationConnector.get(eqTo(currentIntermediary))(any())) thenReturn
            intermediaryRegistrationWithClients(Nil).toFuture
          when(mockIntermediaryRegistrationConnector.get(eqTo(previousIntermediary))(any())) thenReturn
            intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture
          when(mockRegistrationConnector.get(any())(any())) thenReturn
            registrationWrapper.toFuture

          val request = IdentifierRequest(
            FakeRequest(),
            testCredentials,
            vrn,
            enrolmentsWithIntermediaries(Seq(currentIntermediary, previousIntermediary))
          )

          val action = new Harness(
            mockAccountService,
            mockIntermediaryRegistrationConnector,
            mockRegistrationConnector,
            mockAppConfig,
            requestedMaybeIossNumber = Some(iossNumber),
            mockIntermediarySelectedIossNumberRepository
          )

          val result = action.callRefine(request).futureValue

          result mustBe Right(
            expectedRegistrationRequest(request, iossNumber, registrationWrapper, Some(previousIntermediary))
          )
        }
      }
    }

    "must Redirect to NotRegistered page" - {

      "when both IOSS and intermediary enrolment does not exist" in {

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolments
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          requestedMaybeIossNumber = None,
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad()))
      }

      "when both current and previous intermediary enrolments does not have access to IOSS client" in {

        val currentIntermediary = intermediaryNumber
        val previousIntermediary = "IN9007654322"

        when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"
        when(mockIntermediaryRegistrationConnector.get(eqTo(currentIntermediary))(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture
        when(mockIntermediaryRegistrationConnector.get(eqTo(previousIntermediary))(any())) thenReturn
          intermediaryRegistrationWithClients(Nil).toFuture

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolmentsWithIntermediaries(Seq(currentIntermediary, previousIntermediary))
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          requestedMaybeIossNumber = Some("unknown-ioss"),
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad()))
      }

      "when only intermediary is present, requestedMaybeIossNumber is None, and IOSS is not retrieved from the selected repository" in {

        when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn
          intermediaryRegistrationWithClients(Seq(iossNumber)).toFuture
        when(mockIntermediarySelectedIossNumberRepository.get(any())) thenReturn None.toFuture
        when(mockRegistrationConnector.get(any())(any())) thenReturn
          registrationWrapper.toFuture

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolmentsWithIntermediaries(Seq(intermediaryNumber))
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          requestedMaybeIossNumber = None,
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad()))
      }

      "when both intermediary and requestedMaybeIossNumber are not present" in {

        when(mockAppConfig.intermediaryEnrolment) thenReturn "HMRC-IOSS-INT"

        val request = IdentifierRequest(
          FakeRequest(),
          testCredentials,
          vrn,
          enrolmentsWithIntermediaries(Seq.empty)
        )

        val action = new Harness(
          mockAccountService,
          mockIntermediaryRegistrationConnector,
          mockRegistrationConnector,
          mockAppConfig,
          requestedMaybeIossNumber = None,
          mockIntermediarySelectedIossNumberRepository
        )

        val result = action.callRefine(request).futureValue

        result mustBe Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad()))
      }
    }
  }
}
