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
import models.Period
import models.payments._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsServiceImpl @Inject()(
                                 financialDataConnector: FinancialDataConnector,
                                 paymentConnector: PaymentConnector
                               ) extends PaymentsService {
  def prepareFinancialData()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[PrepareData] = {
    financialDataConnector.prepareFinancialData()
  }

  def makePayment(iossNumber: String, period: Period, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnPaymentResponse] = {
    val paymentRequest =
      PaymentRequest(
        iossNumber,
        PaymentPeriod(period.year, period.month),
        (payment.amountOwed * 100).longValue,
        None
      )

    paymentConnector.submit(paymentRequest)
  }
}

trait PaymentsService {
  def prepareFinancialData()(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[PrepareData]

  def makePayment(iossNumber: String, period: Period, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnPaymentResponse]
}