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
import forms.payments.WhichPreviousRegistrationVatPeriodToPayFormProvider
import models.payments.{Payment, PaymentResponse, PaymentStatus, PrepareData}
import models.Period
import models.responses.UnexpectedResponseStatus
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.YourAccountPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentsService
import utils.FutureSyntax.FutureOps
import views.html.payments.{NoPaymentsView, WhichPreviousRegistrationVatPeriodToPayView}

class WhichPreviousRegistrationVatPeriodToPayControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new WhichPreviousRegistrationVatPeriodToPayFormProvider()
  private val form: Form[Period] = formProvider()

  private val mockPaymentsService: PaymentsService = mock[PaymentsService]

  private val prepareData: PrepareData = arbitraryPrepareData.arbitrary.sample.value

  private val duePayments: List[Payment] = prepareData.duePayments
    .map(p => p.copy(paymentStatus = Gen.oneOf(PaymentStatus.values.filter(p => p != PaymentStatus.Unknown)).sample.value))
  private val overduePayments: List[Payment] = prepareData.overduePayments
    .map(p => p.copy(paymentStatus = Gen.oneOf(PaymentStatus.values.filter(p => p != PaymentStatus.Unknown)).sample.value))
  private val excludedPayments: List[Payment] = prepareData.excludedPayments
    .map(p => p.copy(paymentStatus = Gen.oneOf(PaymentStatus.values.filter(p => p != PaymentStatus.Unknown)).sample.value))

  private val payments: List[Payment] = (duePayments ++ overduePayments).sortBy(p => (p.period.year, p.period.month)).reverse

  private lazy val whichPreviousRegistrationVatPeriodToPayRoute = routes.WhichPreviousRegistrationVatPeriodToPayController.onPageLoad(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentsService)
  }

  "WhichPreviousRegistrationVatPeriodToPay Controller" - {

    "must return OK and the correct view for a GET when there are multiple payments due" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = duePayments,
        overduePayments = overduePayments,
        excludedPayments = excludedPayments
      )

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, payments, paymentError = false)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are no payments due or overdue" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = List.empty,
        overduePayments = List.empty,
        excludedPayments = List.empty
      )

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoPaymentsView]

        val redirectUrl: String = YourAccountPage.route(waypoints).url

        status(result) mustBe OK
        contentAsString(result) mustBe view(redirectUrl)(request, messages(application)).toString
      }
    }

    "must redirect to the Pay API for a GET when there is a single payment due" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = List.empty,
        overduePayments = List(overduePayments.head),
        excludedPayments = List.empty
      )

      val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "id", nextUrl = "next-url")

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture
      when(mockPaymentsService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe paymentResponse.nextUrl
      }
    }

    "must show payment error for a GET when there are Unknown PaymentStatus payments present" in {

      val unknownPaymentStatusPayment: Payment = Payment(
        period = arbitraryPeriod.arbitrary.sample.value,
        amountOwed = arbitraryBigDecimal.arbitrary.sample.value,
        dateDue = arbitraryPeriod.arbitrary.sample.value.paymentDeadline,
        paymentStatus = PaymentStatus.Unknown
      )

      val updatedPrepareData: PrepareData = prepareData.copy(
        overduePayments = List(unknownPaymentStatusPayment)
      )

      val payments: List[Payment] = (updatedPrepareData.duePayments ++ updatedPrepareData.overduePayments).sortBy(p => (p.period.year, p.period.month)).reverse

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, payments, paymentError = true)(request, messages(application)).toString
      }
    }

    "must redirect to the Pay API for a POST when there is a single payment due" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = List.empty,
        overduePayments = List(overduePayments.head),
        excludedPayments = List.empty
      )

      val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "id", nextUrl = "next-url")

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture
      when(mockPaymentsService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe paymentResponse.nextUrl
      }
    }

    "must redirect to the Pay API for a chosen period on a POST when there are multiple payments due" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = duePayments,
        overduePayments = overduePayments,
        excludedPayments = excludedPayments
      )

      val chosenPaymentPeriod: Period = updatedPrepareData.overduePayments.head.period
      val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "id", nextUrl = "next-url")

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture
      when(mockPaymentsService.makePayment(any(), eqTo(chosenPaymentPeriod), any())(any())) thenReturn Right(paymentResponse).toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", chosenPaymentPeriod.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe paymentResponse.nextUrl
      }
    }

    "must redirect to the Pay API unavailable page for a POST when the Pay API service is down" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = duePayments,
        overduePayments = overduePayments,
        excludedPayments = excludedPayments
      )

      val chosenPaymentPeriod: Period = updatedPrepareData.overduePayments.head.period

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture
      when(mockPaymentsService.makePayment(any(), eqTo(chosenPaymentPeriod), any())(any())) thenReturn
        Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", chosenPaymentPeriod.toString))

        val frontendAppConfig = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val redirectUrl: String = s"${frontendAppConfig.paymentsBaseUrl}/pay/service-unavailable"

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe redirectUrl
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val updatedPrepareData: PrepareData = prepareData.copy(
        duePayments = duePayments,
        overduePayments = overduePayments,
        excludedPayments = excludedPayments
      )

      val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "id", nextUrl = "next-url")

      when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn updatedPrepareData.toFuture
      when(mockPaymentsService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, payments, paymentError = false)(request, messages(application)).toString
      }
    }
  }
}
