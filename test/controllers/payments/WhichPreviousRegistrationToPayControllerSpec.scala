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

package controllers.payments

import base.SpecBase
import connectors.RegistrationConnector
import controllers.FakeMultipleEnrolmentsGetRegistrationAction
import controllers.actions.GetRegistrationAction
import forms.payments.WhichPreviousRegistrationToPayFormProvider
import models.RegistrationWrapper
import models.payments.{Payment, PaymentStatus, PrepareData}
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import pages.payments.{WhichPreviousRegistrationToPayPage, WhichPreviousRegistrationVatPeriodToPayPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SelectedIossNumberRepository
import services.{PaymentsService, PreviousRegistrationService}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps
import viewmodels.payments.SelectedIossNumber
import views.html.payments.WhichPreviousRegistrationToPayView

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

  private val prepareData: PrepareData = PrepareData(
    duePayments = List(payment),
    overduePayments = List.empty,
    excludedPayments = List.empty,
    totalAmountOwed = BigDecimal(100),
    totalAmountOverdue = BigDecimal(100),
    iossNumber = otherIossNumber
  )

  private lazy val whichPreviousRegistrationToPayRoute: String = routes.WhichPreviousRegistrationToPayController.onPageLoad(waypoints).url

  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockPaymentsService: PaymentsService = mock[PaymentsService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentsService)
    Mockito.reset(mockPreviousRegistrationService)
  }

  "WhichPreviousRegistrationToPay Controller" - {

    "must return OK and the correct view for a GET when there are multiple prepare data objects returned" in {

      val prepareDataList: List[PrepareData] = List(prepareData, prepareData.copy(iossNumber = additioanlIossNumber))

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData()(any())) thenReturn prepareDataList.toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationAction)
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

    // TODO
    "must redirect to makePayment for a GET when there is only one prepare data object with a single payment returned" in {

      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData()(any())) thenReturn List(prepareData).toFuture

      implicit val hc: HeaderCarrier = HeaderCarrier()
      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationAction)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        val redirect = mockPaymentsService
          .makePayment(prepareData.iossNumber, period, prepareData.totalAmountOwed)(hc).futureValue.fold(_, pr => pr.nextUrl)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe redirect
      }
    }

    "must save the iossNumber and redirect to WhichPreviousRegistrationVatPeriodToPayPage for a GET" +
      "when there is only one prepare data object with multiple payment returned" in {

      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, iossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))
      val mockSelectedIossNumberRepository = mock[SelectedIossNumberRepository]

      val prepareDataList = List(
        prepareData,
        prepareData
          .copy(duePayments = List(payment.copy(paymentStatus = PaymentStatus.Unpaid)), iossNumber = additioanlIossNumber)
      )

      when(mockSelectedIossNumberRepository.set(any())) thenReturn selectedIossNumber.toFuture
      when(mockPreviousRegistrationService.getPreviousRegistrationPrepareFinancialData()(any())) thenReturn prepareDataList.toFuture

      val enrolments: Enrolments = Enrolments(Set(enrolment1, enrolment2, enrolment3))
      val fakeMultipleEnrolmentsGetRegistrationAction = new FakeMultipleEnrolmentsGetRegistrationAction(enrolments, registrationWrapper)

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeMultipleEnrolmentsGetRegistrationAction)
      )
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[SelectedIossNumberRepository].toInstance(mockSelectedIossNumberRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints).url
        verify(mockSelectedIossNumberRepository, times(1)).set(selectedIossNumber)
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(WhichPreviousRegistrationToPayPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val view = application.injector.instanceOf[WhichPreviousRegistrationToPayView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(iossNumber), waypoints, List(prepareData))(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val selectedIossNumber: SelectedIossNumber = SelectedIossNumber(userAnswersId, iossNumber, lastUpdated = Instant.now(stubClockAtArbitraryDate))
      val mockSelectedIossNumberRepository = mock[SelectedIossNumberRepository]

      when(mockSelectedIossNumberRepository.set(any())) thenReturn selectedIossNumber.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SelectedIossNumberRepository].toInstance(mockSelectedIossNumberRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", selectedIossNumber.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WhichPreviousRegistrationToPayPage.navigate(waypoints, emptyUserAnswers, emptyUserAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationToPayRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
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
