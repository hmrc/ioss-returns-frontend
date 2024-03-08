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
import models.InvalidJson
import models.payments.PaymentResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PaymentsService, PreviousRegistrationService}
import testUtils.PreviousRegistrationData.previousRegistrations
import utils.FutureSyntax.FutureOps

class PaymentControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockPaymentService: PaymentsService = mock[PaymentsService]
  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]

  private val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "journeyId", nextUrl = "nextUrl")
  private val amount = 20000000

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentService)
    Mockito.reset(mockPreviousRegistrationService)
  }

  "Payment Controller" - {

    "makePayment" - {
      "should make request to pay-api successfully" in {

        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePayment(waypoints, period, amount).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should handle a failed request to pay-api" in {

        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Left(InvalidJson).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePayment(waypoints, period, amount).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith("/pay/service-unavailable")
        }
      }
    }

    "makePaymentForIossNumber" - {
      "should make request to pay-api successfully" in {

        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, amount, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should handle a failed request to pay-api" in {

        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Left(InvalidJson).toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, amount, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith("/pay/service-unavailable")
        }
      }

      "should redirect to Journey recovery when IOSS number is not part of previous registrations or request.iossNumber" in {

        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder()
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, amount, "IM9001111111").url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
        }
      }
    }
  }
}
