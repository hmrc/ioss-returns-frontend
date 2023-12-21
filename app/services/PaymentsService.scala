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
import connectors.{FinancialDataConnector, PaymentConnector, VatReturnConnector}
import models.Period
import models.etmp._
import models.financialdata.FinancialData
import models.payments.{Payment, PaymentPeriod, PaymentRequest}
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import java.time._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsService @Inject()(
                                 financialDataConnector: FinancialDataConnector,
                                 vatReturnConnector: VatReturnConnector,
                                 paymentConnector: PaymentConnector
                               ) {

  def getUnpaidPayments(iossNumber: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Payment]] = {
    withFinancialDataAndVatReturns(iossNumber) {
      (financialDataMaybe, vatReturns) => {
        val vatReturnsForPeriodsWithOutstandingAmounts = filterIfPaymentOutstanding(financialDataMaybe, vatReturns)

        val payments = vatReturnsForPeriodsWithOutstandingAmounts.map(
          vatReturnDue => {
            calculatePayment(
              vatReturnDue, financialDataMaybe)
          }
        )

        payments
      }
    }
  }

  private def withFinancialDataAndVatReturns[T](iossNumber: String)
                                               (block: (Option[FinancialData], List[EtmpVatReturn]) => T)(implicit ec: ExecutionContext, hc: HeaderCarrier) = {
    val beginningOfTime = 1000

    for {
      financialData <- financialDataConnector.getFinancialData(LocalDate.now().minusYears(beginningOfTime)) //Todo: Recheck
      obligations <- vatReturnConnector.getObligations(iossNumber) //Todo: Open/Fullfilled?
      vatReturns <- getVatReturnsForObligations(iossNumber, obligations)
    } yield {
      block(Some(financialData), vatReturns)
    }
  }

  def getVatReturnsForObligations(iossNumber: String, obligations: EtmpObligations)
                                 (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[EtmpVatReturn]] =
    Future.sequence(
      obligations.getPeriods().map { period =>
        vatReturnConnector.get(period).map {
          case Right(vatReturn) => List(vatReturn)
          case Left(e) if e.body startsWith "UNEXPECTED_404" => Nil
        }
      }
    ).map(_.flatten)

  def filterIfPaymentOutstanding(financialDataMaybe: Option[FinancialData], etmpVatReturns: List[EtmpVatReturn]): List[EtmpVatReturn] = {
    etmpVatReturns.filter(vatReturn => {
      val charge = financialDataMaybe.flatMap(_.getChargeForPeriod(Period.fromKey(vatReturn.periodKey)))

      // If some payments "FOR THAT PERIOD" made with outstanding amount
      // or no payments made "FOR THAT PERIOD" but there is vat amount "FOR THAT PERIOD"
      val hasChargeWithOutstanding = charge.exists(_.outstandingAmount > 0)
      val expectingCharge = charge.isEmpty && (vatReturn.getTotalVatOnSalesAfterCorrection() > 0)

      hasChargeWithOutstanding || expectingCharge
    })
  }

  def calculatePayment(vatReturn: EtmpVatReturn, financialDataMaybe: Option[FinancialData]): Payment = {
    val period = Period.fromKey(vatReturn.periodKey)
    val charge = for {
      financialData <- financialDataMaybe
      chargeCalculated <- financialData.getChargeForPeriod(period)
    } yield chargeCalculated

    val paymentStatus = charge.getPaymentStatus()

    Payment(period,
      charge.map(c => c.outstandingAmount).getOrElse(vatReturn.getTotalVatOnSalesAfterCorrection()),
      period.paymentDeadline,
      paymentStatus
    )
  }

  def makePayment(vrn: Vrn, period: Period, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[ReturnPaymentResponse] = {
    val paymentRequest =
      PaymentRequest(
        vrn,
        PaymentPeriod(period.year, period.month),
        (payment.amountOwed * 100).longValue
      )

    paymentConnector.submit(paymentRequest)
  }
}