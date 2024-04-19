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

package services

import connectors.VatReturnConnector
import logging.Logging
import models.Period
import models.payments._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PeriodWithFinancialDataService @Inject()(
                                                paymentsService: PaymentsService,
                                                obligationsService: ObligationsService,
                                                vatReturnConnector: VatReturnConnector,
                                              )(implicit ec: ExecutionContext) extends Logging {

  def getPeriodWithFinancialData(iossNumber: String)(implicit hc: HeaderCarrier): Future[Map[Int, Seq[(Period, Payment)]]] = {
    for {
      obligations <- obligationsService.getFulfilledObligations(iossNumber)
      preparedFinancialData <- paymentsService.prepareFinancialDataWithIossNumber(iossNumber)
      periods = obligations.map(_.periodKey).map(Period.fromKey)
      allUnpaidPayments = preparedFinancialData.duePayments ++ preparedFinancialData.overduePayments ++ preparedFinancialData.excludedPayments
      allPaymentsForPeriod <- getAllPaymentsForPeriods(periods, allUnpaidPayments)
    } yield allPaymentsForPeriod.flatten.groupBy(_._1.year)
  }

  private def getAllPaymentsForPeriods(periods: Seq[Period], allUnpaidPayments: List[Payment])
                                      (implicit hc: HeaderCarrier): Future[Seq[Map[Period, Payment]]] = {

    Future.sequence(periods.map { period =>
      allUnpaidPayments.find(_.period == period) match {
        case Some(payment) =>
          Future(Map(period -> payment))
        case _ =>
          vatReturnConnector.get(period).map {
            case Right(vatReturn) =>
              val paymentStatus = if (vatReturn.correctionPreviousVATReturn.isEmpty && vatReturn.goodsSupplied.isEmpty) {
                PaymentStatus.NilReturn
              } else {
                PaymentStatus.Paid
              }
              Map(period -> Payment(
                period = period,
                amountOwed = 0,
                dateDue = period.paymentDeadline,
                paymentStatus = paymentStatus
              ))
            case Left(error) =>
              val exception = new IllegalStateException(s"Unable to get vat return for calculating amount owed ${error.body}")
              logger.error(exception.getMessage, exception)
              throw exception
          }
      }
    })
  }
}
