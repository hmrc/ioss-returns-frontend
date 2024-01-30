/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.PaymentHttpParser.ReturnPaymentResponse
import connectors.{FinancialDataConnector, PaymentConnector}
import logging.Logging
import models.Period
import models.payments._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject()(
                                 financialDataConnector: FinancialDataConnector,
                                 paymentConnector: PaymentConnector
                               ) extends Logging {
  def prepareFinancialData()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[PrepareData] = {
    financialDataConnector.prepareFinancialData().map {
      case Right(preparedData) => preparedData
      case Left(error) =>
        val message = s"There was a problem getting prepared financial data ${error.body}"
        logger.error(message)
        throw new Exception(message)
    }
  }

  def makePayment(iossNumber: String, period: Period, amountOwed: BigDecimal)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnPaymentResponse] = {
    val paymentRequest =
      PaymentRequest(
        iossNumber,
        PaymentPeriod(period.year, period.month, period.paymentDeadline),
        (amountOwed * 100).longValue,
        Some(period.paymentDeadline)
      )

    paymentConnector.submit(paymentRequest)
  }
}
