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

package services

import base.SpecBase
import connectors.{FinancialDataConnector, PaymentConnector}
import models.UnexpectedResponseStatus
import models.payments.{PaymentResponse, PrepareData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class PaymentsServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]
  private val mockPaymentConnector: PaymentConnector = mock[PaymentConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockPaymentConnector)
  }

  "PaymentService" - {

    ".prepareFinancialData" - {

      "must return PrepareData when connector successfully returns Right(PrepareData)" in {

        val prepareData: PrepareData = arbitraryPrepareData.arbitrary.sample.value

        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn Right(prepareData).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val result = service.prepareFinancialData().futureValue

        result mustBe prepareData
      }

      "must throw an exception when connector returns Left(error)" in {

        when(mockFinancialDataConnector.prepareFinancialData()(any())) thenReturn
          Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val result = service.prepareFinancialData().failed

        whenReady(result) { exp =>

          exp mustBe a[Exception]
          exp.getMessage mustBe exp.getMessage
        }
      }
    }

    ".prepareFinancialDataWithIossNumber" - {

      "must return PrepareData when connector successfully returns Right(PrepareData)" in {

        val prepareData: PrepareData = arbitraryPrepareData.arbitrary.sample.value

        when(mockFinancialDataConnector.prepareFinancialDataWithIossNumber(any())(any())) thenReturn Right(prepareData).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val result = service.prepareFinancialDataWithIossNumber(prepareData.iossNumber).futureValue

        result mustBe prepareData
      }

      "must throw an exception when connector returns Left(error)" in {

        val prepareData: PrepareData = arbitraryPrepareData.arbitrary.sample.value

        when(mockFinancialDataConnector.prepareFinancialDataWithIossNumber(any())(any())) thenReturn
          Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val result = service.prepareFinancialDataWithIossNumber(prepareData.iossNumber).failed

        whenReady(result) { exp =>

          exp mustBe a[Exception]
          exp.getMessage mustBe exp.getMessage
        }
      }
    }

    ".makePayment" - {

      "must return Right(PaymentResponse) when valid payment request submitted" in {

        val paymentResponse: PaymentResponse = PaymentResponse(journeyId = "", nextUrl = "next-url")

        when(mockPaymentConnector.submit(any())(any())) thenReturn Right(paymentResponse).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val amountOwed = arbitraryBigDecimal.arbitrary.sample.value
        val result = service.makePayment(iossNumber, period, amountOwed).futureValue

        result mustBe Right(paymentResponse)
      }

      "must return Left(error) when error response received from connector" in {

        when(mockPaymentConnector.submit(any())(any())) thenReturn
          Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

        val service = new PaymentsService(mockFinancialDataConnector, mockPaymentConnector)

        val amountOwed = arbitraryBigDecimal.arbitrary.sample.value
        val result = service.makePayment(iossNumber, period, amountOwed).futureValue

        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error"))
      }
    }
  }
}
