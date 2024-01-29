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

package controllers.previousReturns

import connectors.VatReturnConnector
import controllers.actions._
import logging.Logging
import models.Period
import models.payments.{Payment, PaymentStatus}
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ObligationsService, PaymentsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousReturns.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnsHistoryController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   paymentsService: PaymentsService,
                                                   obligationsService: ObligationsService,
                                                   vatReturnConnector: VatReturnConnector,
                                                   view: SubmittedReturnsHistoryView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      for {
        obligations <- obligationsService.getFulfilledObligations(request.iossNumber)
        preparedFinancialData <- paymentsService.prepareFinancialData()
        periods = obligations.map(_.periodKey).map(Period.fromKey)
        allUnpaidPayments = preparedFinancialData.duePayments ++ preparedFinancialData.overduePayments
        periodWithFinancialData <- getPeriodWithFinancialData(periods, allUnpaidPayments)
      } yield {

        Ok(view(waypoints, periodWithFinancialData))
      }
  }

  private def getPeriodWithFinancialData(periods: Seq[Period], allUnpaidPayments: List[Payment])
                                        (implicit hc: HeaderCarrier): Future[Map[Int, Seq[(Period, Payment)]]] = {
    val futurePeriods = Future(periods)

    for {
      periods <- futurePeriods
      allPaymentsForPeriod <- getAllPaymentsForPeriods(periods, allUnpaidPayments)
    } yield allPaymentsForPeriod.flatten.groupBy(_._1.year)
  }

  private def getAllPaymentsForPeriods(periods: Seq[Period], allUnpaidPayments: List[Payment])
                                      (implicit hc: HeaderCarrier): Future[Seq[Map[Period, Payment]]] = {

    println("allUnpaidPayments " + allUnpaidPayments)
    Future.sequence(periods.map { period =>
      allUnpaidPayments.find(_.period == period) match {
        case Some(payment) =>
          println(s"Payment: ${payment}")
          Future(Map(period -> payment))
        case _ =>
          vatReturnConnector.get(period).map {
            case Right(vatReturn) =>
              println(s"VAT_RETURN: ${vatReturn}")
              val paymentStatus = if (vatReturn.correctionPreviousVATReturn.isEmpty && vatReturn.goodsSupplied.isEmpty) {
                println(s"paymentStatus = NilReturn with ${vatReturn.totalVATAmountDueForAllMSGBP} and ${vatReturn.totalVATAmountPayable}")
                PaymentStatus.NilReturn
//              } else if (vatReturn.totalVATAmountPayable == 0) {
//                println(s"paymentStatus = Paid with ${vatReturn.totalVATAmountDueForAllMSGBP}")
//                PaymentStatus.Paid
              } else {
                println(s"paymentStatus = Unknown with ${vatReturn.totalVATAmountDueForAllMSGBP}")
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
