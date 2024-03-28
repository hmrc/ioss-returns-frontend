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
import connectors.{FinancialDataConnector, VatReturnConnector}
import connectors.FinancialDataHttpParser.ChargeResponse
import models.{InvalidJson, UnexpectedResponseStatus}
import models.financialdata.Charge
import models.payments.PaymentResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, Mockito}
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{PaymentsService, PreviousRegistrationService}
import testUtils.EtmpVatReturnData.etmpVatReturn
import testUtils.PreviousRegistrationData.previousRegistrations
import utils.FutureSyntax.FutureOps

class PaymentControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockPaymentService: PaymentsService = mock[PaymentsService]
  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  private val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "journeyId", nextUrl = "nextUrl")
  private val amount = 20000000
  private val chargeResponse: ChargeResponse = Right(Some(Charge(
    period = period,
    originalAmount = amount,
    outstandingAmount = amount,
    clearedAmount = BigDecimal(0)
  )))
  private val errorChargeResponse: ChargeResponse = Left(UnexpectedResponseStatus(status = 500, body = "error"))

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentService)
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockVatReturnConnector)
  }

  "Payment Controller" - {

    "makePayment" - {
      "should make request to pay-api successfully" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn chargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePayment(waypoints, period).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should make request to pay-api successfully when financial data endpoint is down" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn errorChargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture
        when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(etmpVatReturn).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePayment(waypoints, period).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should handle a failed request to pay-api" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn chargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Left(InvalidJson).toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePayment(waypoints, period).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith("/pay/service-unavailable")
        }
      }
    }

    "makePaymentForIossNumber" - {
      "should make request to pay-api successfully" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn chargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should make request to pay-api successfully even when financial data is down" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn errorChargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Right(paymentResponse).toFuture
        when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(etmpVatReturn).toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith(paymentResponse.nextUrl)
        }
      }

      "should handle a failed request to pay-api" in {

        when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn chargeResponse.toFuture
        when(mockPaymentService.makePayment(any(), any(), any())(any())) thenReturn Left(InvalidJson).toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[PaymentsService].toInstance(mockPaymentService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must endWith("/pay/service-unavailable")
        }
      }

      "should redirect to Journey recovery when IOSS number is not part of previous registrations or request.iossNumber" in {

        when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

        val application = applicationBuilder()
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.PaymentController.makePaymentForIossNumber(waypoints, period, "IM9001111111").url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
        }
      }
    }
  }
}
