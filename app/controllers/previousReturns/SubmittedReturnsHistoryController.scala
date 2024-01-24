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

import connectors.FinancialDataConnector
import controllers.actions._
import models.Period
import models.payments.Payment
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ObligationsService, PaymentsService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousReturns.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmittedReturnsHistoryController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   paymentsService: PaymentsService,
                                                   obligationsService: ObligationsService,
                                                   view: SubmittedReturnsHistoryView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      // TODO -> get etmp vat return to determine payment if financial data api down
      for {
        obligations <- obligationsService.getFulfilledObligations(request.iossNumber)
        preparedFinancialData <- paymentsService.prepareFinancialData()
      } yield {

        val periods = obligations.map(_.periodKey).map(Period.fromKey)

        val allPayments = preparedFinancialData.duePayments ++ preparedFinancialData.overduePayments

        println(s"ALL PAYMENTS: $allPayments")

        // TODO if head of empty list????
        val periodWithFinancialData: Map[Int, Seq[(Period, Payment)]] = periods.flatMap { period =>
          allPayments.find(_.period == period) match {
            case Some(payment) => Map(period -> payment)
            case _ => Map.empty
          }
        }.groupBy(_._1.year)

        println(s"periodWithFinancialData: $periodWithFinancialData")

        Ok(view(waypoints, periodWithFinancialData))
      }
  }
}
