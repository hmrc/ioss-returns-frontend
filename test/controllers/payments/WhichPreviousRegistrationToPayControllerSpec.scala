/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.payments

import base.SpecBase
import config.FrontendAppConfig
import connectors.{IntermediaryRegistrationConnector, RegistrationConnector}
import controllers.actions.{GetRegistrationAction, GetRegistrationActionProvider}
import forms.payments.WhichPreviousRegistrationToPayFormProvider
import models.payments.{Payment, PaymentResponse, PaymentStatus, PrepareData}
import models.requests.{IdentifierRequest, RegistrationRequest}
import models.{RegistrationWrapper, StandardPeriod}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.payments.WhichPreviousRegistrationVatPeriodToPayPage
import pages.{JourneyRecoveryPage, YourAccountPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.{IntermediarySelectedIossNumberRepository, SelectedIossNumberRepository}
import services.{AccountService, PaymentsService, PreviousRegistrationService}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps
import viewmodels.payments.SelectedIossNumber
import views.html.payments.{NoPaymentsView, WhichPreviousRegistrationToPayView}

import java.time.{Instant, LocalDate}
import scala.concurrent.{ExecutionContext, Future}

class WhichPreviousRegistrationToPayControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new WhichPreviousRegistrationToPayFormProvider()
  private val form: Form[String] = formProvider()

  private val otherIossNumber: String = "IM9001234123"
  private val additioanlIossNumber: String = "IM9001231234"
  private val enrolment1: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", iossNumber)), "test", None)
  private val enrolment2: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", otherIossNumber)), "test", None)
  private val enrolment3: Enrolment = Enrolment(iossEnrolmentKey, Seq(EnrolmentIdentifier("IOSSNumber", additioanlIossNumber)), "test", None)

  private val payment: Payment = Payment(
    period = period,
    amountOwed = BigDecimal(100),
    dateDue = LocalDate.now(),
    paymentStatus = PaymentStatus.Partial
  )

  private val payment2: Payment = Payment(
    period = StandardPeriod(period.year, period.month.minus(1)),
    amountOwed = BigDecimal(250),
    dateDue = payment.dateDue.minusMonths(1),
    paymentStatus = PaymentStatus.Unpaid
  )

  private val prepareData: PrepareData = PrepareData(
    duePayments = List(payment),
    overduePayments = List.empty,
    excludedPayments = List.empty,
    totalAmountOwed = BigDecimal(100),
    totalAmountOverdue = BigDecimal(100),
    iossNumber = otherIossNumber
  )

  private val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "journeyId", nextUrl = "nextUrl")

  private lazy val whichPreviousRegistrationToPayRoute: String = routes.WhichPreviousRegistrationToPayController.onPageLoad(waypoints).url

  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockPaymentsService: PaymentsService = mock[PaymentsService]

  private val mockSelectedIossNumberRepository: SelectedIossNumberRepository = mock[SelectedIossNumberRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentsService)
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(mockSelectedIossNumberRepository)
  }

  "WhichPreviousRegistrationToPay Controller" - {

    "must return OK and the correct view for a GET when there are multiple prepare data objects returned" in {

      val prepareDataList: List[PrepareData] = List(prepareData, prepareData.copy(iossNumber = additioanlIossNumber))

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn prepareDataList.toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationActionProvider = new FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationActionProvider)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhichPreviousRegistrationToPayView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, prepareDataList)(request, messages(application)).toString
      }
    }

    "must return OK and the no payments view for a GET when there are no payments due or overdue" in {

      val nothingDuePrepareData = PrepareData(
        duePayments = List.empty,
        overduePayments = List.empty,
        excludedPayments = List.empty,
        totalAmountOwed = BigDecimal(0),
        totalAmountOverdue = BigDecimal(0),
        iossNumber = otherIossNumber
      )

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn List(nothingDuePrepareData).toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationActionProvider = new FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationActionProvider)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoPaymentsView]

        val redirectUrl: String = YourAccountPage.route(waypoints).url

        status(result) mustBe OK
        contentAsString(result) mustBe view(redirectUrl)(request, messages(application)).toString
      }
    }

    "must redirect to makePayment for a GET when there is only one prepare data object with a single payment returned" in {

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn List(prepareData).toFuture
      when(mockPaymentsService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationActionProvider = new FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationActionProvider)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        val redirect = paymentResponse.nextUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe redirect
      }
    }

    "must save the iossNumber and redirect to WhichPreviousRegistrationVatPeriodToPayPage for a GET" +
      " when there is only one prepare data object with multiple payments returned" in {

      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, additioanlIossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))

      val prepareDataList = List(
        prepareData
          .copy(
            overduePayments = List(payment2),
            totalAmountOwed = prepareData.totalAmountOwed + payment2.amountOwed,
            totalAmountOverdue = payment2.amountOwed,
            iossNumber = additioanlIossNumber
          )
      )

      when(mockSelectedIossNumberRepository.set(any())) thenReturn selectedIossNumber.toFuture
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn prepareDataList.toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationActionProvider = new FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationActionProvider)
      )
        .overrides(bind[SelectedIossNumberRepository].toInstance(mockSelectedIossNumberRepository))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints).url
      }
    }

    "must throw an Exception for a GET when there is no prepare financial data retrieved" in {

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn List.empty.toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationActionProvider = new FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationActionProvider)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        val exceptionMessage = "There was an issue retrieving prepared financial data"

        whenReady(result.failed) { exp =>

          exp mustBe a[Exception]
          exp.getMessage mustEqual exceptionMessage
        }
      }
    }

    "must redirect to the make payment page when valid data is submitted and there is a single payment present" in {

      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, otherIossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn List(prepareData).toFuture
      when(mockPaymentsService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", selectedIossNumber.iossNumber))

        val result = route(application, request).value

        val redirect = paymentResponse.nextUrl

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe redirect
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted and there are multiple payments present" in {

      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, additioanlIossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))

      val prepareDataList = List(
        prepareData
          .copy(
            overduePayments = List(payment2),
            totalAmountOwed = prepareData.totalAmountOwed + payment2.amountOwed,
            totalAmountOverdue = payment2.amountOwed,
            iossNumber = additioanlIossNumber
          )
      )

      when(mockSelectedIossNumberRepository.set(any())) thenReturn selectedIossNumber.toFuture
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn prepareDataList.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SelectedIossNumberRepository].toInstance(mockSelectedIossNumberRepository))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", selectedIossNumber.iossNumber))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery page on a POST when the selected OSS number IOSS number has no matching prepared financial data" in {

      val iossNumber = "IM9007654321"
      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, iossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))

      val prepareDataList = List(prepareData)

      when(mockSelectedIossNumberRepository.set(any())) thenReturn selectedIossNumber.toFuture
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn prepareDataList.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SelectedIossNumberRepository].toInstance(mockSelectedIossNumberRepository))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", selectedIossNumber.iossNumber))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData(any())(any())) thenReturn List(prepareData).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhichPreviousRegistrationToPayView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, List(prepareData))(request, messages(application)).toString
      }
    }
  }
}

class FakeMultipleEnrolmentsGetRegistrationAction(enrolments: Enrolments, registration: RegistrationWrapper) extends GetRegistrationAction(
  mock[AccountService],
  mock[IntermediaryRegistrationConnector],
  mock[RegistrationConnector],
  mock[FrontendAppConfig],
  None,
  mock[IntermediarySelectedIossNumberRepository]
)(ExecutionContext.Implicits.global) {

  override def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
    Right(RegistrationRequest(request.request, request.credentials, Some(request.vrn), "Company Name", "IM9001234567", registration, None, enrolments)).toFuture
}

class FakeMultipleEnrolmentsGetRegistrationActionProvider(enrolments: Enrolments, registrationWrapper: RegistrationWrapper) extends GetRegistrationActionProvider(
  mock[AccountService],
  mock[IntermediaryRegistrationConnector],
  mock[RegistrationConnector],
  mock[IntermediarySelectedIossNumberRepository],
  mock[FrontendAppConfig]
)(ExecutionContext.Implicits.global) {
  override def apply(maybeIossNumber: Option[String] = None): GetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)
}
