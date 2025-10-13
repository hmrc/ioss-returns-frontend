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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import connectors.{FinancialDataConnector, RegistrationConnector, ReturnStatusConnector, SaveForLaterConnector}
import controllers.actions.GetRegistrationAction
import generators.Generators
import models.SubmissionStatus.*
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.payments.{Payment, PaymentStatus, PrepareData}
import models.requests.{IdentifierRequest, RegistrationRequest}
import models.{RegistrationWrapper, StandardPeriod, SubmissionStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.PreviousRegistrationService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps
import viewmodels.PaymentsViewModel
import viewmodels.yourAccount.{CurrentReturns, Return, ReturnsViewModel}
import views.html.YourAccountView

import java.time.{Clock, LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  private val nextPeriod: StandardPeriod = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.APRIL)
  private val otherIossNumber: String = "IM9001234123"
  private val enrolment1: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)), "test", None)
  private val enrolment2: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", otherIossNumber)), "test", None)

  private def createRegistrationWrapperWithExclusion(effectiveDate: LocalDate): RegistrationWrapper = {
    val registration = registrationWrapper.registration

    registrationWrapper
      .copy(vatInfo = registrationWrapper.vatInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate))))
      .copy(
      registration = registration.copy(
        exclusions = List(
          EtmpExclusion(
            exclusionReason = NoLongerSupplies,
            effectiveDate = effectiveDate,
            decisionDate = LocalDate.now(),
            quarantine = false
          )
        )
      )
    )
  }

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]
  private val mockFinancialDataConnector = mock[FinancialDataConnector]
  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val saveForLaterConnector = mock[SaveForLaterConnector]
  private val registrationConnector = mock[RegistrationConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockReturnStatusConnector)
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(saveForLaterConnector)
    Mockito.reset(registrationConnector)
  }

  "Your Account Controller" - {

    "should display your account view" - {

      "when registration wrapper is present" in {

        val now = LocalDate.now()
        val periodOverdue1 = StandardPeriod(now.minusYears(1).getYear, Month.JANUARY)
        val periodOverdue2 = StandardPeriod(now.minusYears(1).getYear, Month.FEBRUARY)
        val periodDue1 = StandardPeriod(now.getYear, now.getMonth.plus(1))
        val periodDue2 = StandardPeriod(now.getYear, now.getMonth.plus(2))
        val amountOwed = 10
        val paymentOverdue1 = Payment(periodOverdue1, amountOwed, periodOverdue1.paymentDeadline, PaymentStatus.Unpaid)
        val paymentOverdue2 = Payment(periodOverdue2, amountOwed, periodOverdue2.paymentDeadline, PaymentStatus.Unpaid)
        val paymentDue1 = Payment(periodDue1, amountOwed, periodDue1.paymentDeadline, PaymentStatus.Unpaid)
        val paymentDue2 = Payment(periodDue2, amountOwed, periodDue2.paymentDeadline, PaymentStatus.Unpaid)
        val prepareData = PrepareData(List(paymentDue1, paymentDue2),
          List(paymentOverdue1, paymentOverdue2),
          List.empty,
          List(paymentDue1,
            paymentDue2,
            paymentOverdue1,
            paymentOverdue2
          ).map(_.amountOwed).sum,
          List(paymentOverdue1, paymentOverdue2).map(_.amountOwed).sum,
          iossNumber)
        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn Right(prepareData).toFuture

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(Return(
              nextPeriod,
              nextPeriod.firstDay,
              nextPeriod.lastDay,
              nextPeriod.paymentDeadline,
              SubmissionStatus.Next,
              inProgress = false,
              isOldest = false
            )),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapper)
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].to(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustBe OK
        }
      }
    }

    "must return OK with leaveThisService link and without cancelYourRequestToLeave link when a trader is not excluded" in {

      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Next,
            inProgress = false,
            isOldest = false
          )),
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          Some(appConfig.leaveThisServiceUrl),
          None,
          exclusionsEnabled = true,
          None,
          hasSubmittedFinalReturn = false,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List.empty,
          hasDeregisteredFromVat = false,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }

    "must return OK with rejoinThisService link" in {

      val registrationWrapperWithExclusion: RegistrationWrapper = createRegistrationWrapperWithExclusion(LocalDate.now())

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Complete,
            inProgress = false,
            isOldest = false
          )),
          finalReturnsCompleted = true
        )).toFuture

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapperWithExclusion,
        clock = Some(Clock.systemUTC()))
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapperWithExclusion.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          Some(appConfig.rejoinThisServiceUrl),
          None,
          None,
          exclusionsEnabled = true,
          Some(registrationWrapperWithExclusion.registration.exclusions.head),
          hasSubmittedFinalReturn = true,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List.empty,
          hasDeregisteredFromVat = true,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }

    "must return OK with no rejoinThisService link when there is an outstanding return, which is within 3 years from due date" in {

      val registrationWrapperWithExclusion: RegistrationWrapper = createRegistrationWrapperWithExclusion(LocalDate.now())

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Due,
            inProgress = false,
            isOldest = true
          )),
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapperWithExclusion,
        clock = Some(Clock.systemUTC()))
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapperWithExclusion.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          None,
          None,
          exclusionsEnabled = true,
          Some(registrationWrapperWithExclusion.registration.exclusions.head),
          hasSubmittedFinalReturn = false,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, SubmissionStatus.Due, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List.empty,
          hasDeregisteredFromVat = true,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }

    "must return OK with cancelYourRequestToLeave link and without leaveThisService link when a trader is excluded" in {

      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val exclusion = EtmpExclusion(
        NoLongerSupplies,
        LocalDate.now(stubClockAtArbitraryDate).plusDays(2),
        LocalDate.now(stubClockAtArbitraryDate).minusDays(1),
        quarantine = false
      )

      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper
          .copy(vatInfo = registrationWrapper.vatInfo.copy(deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate))))
          .copy(registration = registrationWrapper.registration.copy(exclusions = Seq(exclusion)))

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Next,
            inProgress = false,
            isOldest = false
          )), finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          None,
          Some(appConfig.cancelYourRequestToLeaveUrl),
          exclusionsEnabled = true,
          maybeExclusion = Some(exclusion),
          hasSubmittedFinalReturn = false,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List.empty,
          hasDeregisteredFromVat = true,
          "https://test-url.com"
        )(request, messages(application)).toString
      }
    }

    "must return OK with Pay for a previous registration link when there is one additional previous registration that has outstanding payments" in {

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2))

      val fakeMultipleEnrolmentsGetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)

      val duePayment: Payment = Payment(
        period = nextPeriod,
        amountOwed = BigDecimal(1000),
        dateDue = nextPeriod.paymentDeadline,
        paymentStatus = PaymentStatus.Unpaid
      )

      val overduePayment: Payment = Payment(
        period = period,
        amountOwed = BigDecimal(1500),
        dateDue = period.paymentDeadline,
        paymentStatus = PaymentStatus.Partial
      )

      val previousRegistrationPrepareData: PrepareData = PrepareData(
        duePayments = List(duePayment),
        overduePayments = List(overduePayment),
        excludedPayments = List.empty,
        totalAmountOwed = BigDecimal(2500),
        totalAmountOverdue = BigDecimal(1500),
        iossNumber = otherIossNumber
      )

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData()(any())) thenReturn List(previousRegistrationPrepareData).toFuture
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture
      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Next,
            inProgress = false,
            isOldest = false
          )),
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapper,
        clock = Some(Clock.systemUTC()),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationAction)
      )
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          Some(appConfig.leaveThisServiceUrl),
          None,
          exclusionsEnabled = true,
          None,
          hasSubmittedFinalReturn = false,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List(previousRegistrationPrepareData),
          hasDeregisteredFromVat = false,
          "https://test-url.com"
        )(request, messages(application)).toString

        contentAsString(result).contains(messages(application).messages("yourAccount.previousRegistrations.payLink"))
        contentAsString(result)
          .contains(messages(application)
            .messages("yourAccount.previousRegistrations.singular", previousRegistrationPrepareData.totalAmountOwed, otherIossNumber))
      }
    }

    "must return OK with Pay for a previous registration link when there are multiple additional previous registration that has outstanding payments" in {

      val additionalIossNumber: String = "IM9001231234"
      val enrolment3: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", additionalIossNumber)), "test", None)
      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))

      val fakeMultipleEnrolmentsGetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)

      val duePayment: Payment = Payment(
        period = nextPeriod,
        amountOwed = BigDecimal(1000),
        dateDue = nextPeriod.paymentDeadline,
        paymentStatus = PaymentStatus.Unpaid
      )

      val overduePayment: Payment = Payment(
        period = period,
        amountOwed = BigDecimal(1500),
        dateDue = period.paymentDeadline,
        paymentStatus = PaymentStatus.Partial
      )

      val previousRegistrationPrepareData: PrepareData = PrepareData(
        duePayments = List(duePayment),
        overduePayments = List(overduePayment),
        excludedPayments = List.empty,
        totalAmountOwed = BigDecimal(2500),
        totalAmountOverdue = BigDecimal(1500),
        iossNumber = otherIossNumber
      )

      when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData()(any())) thenReturn List(previousRegistrationPrepareData).toFuture
      when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
        Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture
      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            nextPeriod,
            nextPeriod.firstDay,
            nextPeriod.lastDay,
            nextPeriod.paymentDeadline,
            SubmissionStatus.Next,
            inProgress = false,
            isOldest = false
          )),
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapper,
        clock = Some(Clock.systemUTC()),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationAction)
      )
        .configure("urls.userResearch1" -> "https://test-url.com")
        .overrides(
          bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
          bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
          bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService),
          bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
          bind[RegistrationConnector].toInstance(registrationConnector)
        ).build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          waypoints,
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          Some(appConfig.leaveThisServiceUrl),
          None,
          exclusionsEnabled = true,
          None,
          hasSubmittedFinalReturn = false,
          ReturnsViewModel(
            returns = Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            ),
            excludedReturns = Seq.empty,
            stubClockAtArbitraryDate
          )(messages(application)),
          List(previousRegistrationPrepareData),
          hasDeregisteredFromVat = false,
          "https://test-url.com"
        )(request, messages(application)).toString

        contentAsString(result).contains(messages(application).messages("yourAccount.previousRegistrations.payLink"))
        contentAsString(result)
          .contains(messages(application)
            .messages("yourAccount.previousRegistrations.plural", previousRegistrationPrepareData.totalAmountOwed))
      }
    }

    "must return OK and the correct view" - {

      "when there are no returns due" in {

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(Return(
              nextPeriod,
              nextPeriod.firstDay,
              nextPeriod.lastDay,
              nextPeriod.paymentDeadline,
              SubmissionStatus.Next,
              inProgress = false,
              isOldest = false
            )),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due" in {

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(Return.fromPeriod(period, Due, inProgress = true, isOldest = false
            )),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(period, Due, inProgress = true, isOldest = false)
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "and 1 return overdue" in {

        val firstPeriod = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.JANUARY)
        val secondPeriod = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.FEBRUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(
              Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
              Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
            ),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true)
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return overdue" in {

        val period = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.JANUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(
              Return.fromPeriod(period, Overdue, inProgress = false, isOldest = true),
            ),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(period, Overdue, inProgress = false, isOldest = true)
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "when there is 2 returns overdue" in {

        val firstPeriod = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.JANUARY)
        val secondPeriod = StandardPeriod(LocalDate.now.minusYears(1).getYear, Month.FEBRUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(
              Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
              Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false)
            ),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "when there is multiple returns overdue and one due" in {

        val periodYear = 2023
        val firstPeriod = StandardPeriod(periodYear, Month.JANUARY)
        val secondPeriod = StandardPeriod(periodYear, Month.FEBRUARY)
        val thirdPeriod = StandardPeriod(periodYear, Month.MARCH)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(
              Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
              Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
              Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
            ),
            finalReturnsCompleted = false
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
                Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
              ),
              excludedReturns = Seq.empty,
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }

      "when there is multiple returns overdue, one due and one excluded return" in {

        val periodYear = 2023
        val excludedYear = 2019
        val firstPeriod = StandardPeriod(periodYear, Month.JANUARY)
        val secondPeriod = StandardPeriod(periodYear, Month.FEBRUARY)
        val thirdPeriod = StandardPeriod(periodYear, Month.MARCH)
        val excludedPeriod = StandardPeriod(excludedYear, Month.MARCH)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(saveForLaterConnector.get()(any())) thenReturn Future.successful(Right(None))
        when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Right(CurrentReturns(
            Seq(
              Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
              Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
              Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
            ),
            finalReturnsCompleted = false,
            completeOrExcludedReturns = Seq(
              Return.fromPeriod(excludedPeriod, Excluded, inProgress = false, isOldest = true)
            )
          )).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .configure("urls.userResearch1" -> "https://test-url.com")
          .overrides(
            bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector),
            bind[FinancialDataConnector].toInstance(mockFinancialDataConnector),
            bind[SaveForLaterConnector].toInstance(saveForLaterConnector),
            bind[RegistrationConnector].toInstance(registrationConnector)
          ).build()

        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(application))
        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Right(PrepareData(List.empty, List.empty, List.empty, 0, 0, iossNumber)).toFuture

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            waypoints,
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            None,
            Some(appConfig.leaveThisServiceUrl),
            None,
            exclusionsEnabled = true,
            maybeExclusion = None,
            hasSubmittedFinalReturn = false,
            ReturnsViewModel(
              returns = Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
                Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
              ),
              excludedReturns = Seq(Return.fromPeriod(excludedPeriod, Excluded, inProgress = false, isOldest = true)),
              stubClockAtArbitraryDate
            )(messages(application)),
            List.empty,
            hasDeregisteredFromVat = false,
            "https://test-url.com"
          )(request, messages(application)).toString
        }
      }
    }
  }
}

class FakeMultipleEnrolmentsGetRegistrationAction(enrolments: Enrolments, registration: RegistrationWrapper) extends GetRegistrationAction(
  mock[RegistrationConnector]
)(ExecutionContext.Implicits.global) {

  override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
    Right(RegistrationRequest(request.request, request.credentials, request.vrn, request.iossNumber, registration, enrolments)).toFuture
}