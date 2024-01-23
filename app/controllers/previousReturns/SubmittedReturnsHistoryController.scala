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
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.previousReturns.SubmittedReturnsHistoryView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmittedReturnsHistoryController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   cc: AuthenticatedControllerComponents,
                                                   financialDataConnector: FinancialDataConnector,
                                                   obligationsService: ObligationsService,
                                                   view: SubmittedReturnsHistoryView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      for {
        obligations <- obligationsService.getFulfilledObligations(request.iossNumber)
        preparedFinancialData <- financialDataConnector.prepareFinancialData()
      } yield {

        val periods = obligations.map(_.periodKey).map(Period.fromKey)

        val allPayments = preparedFinancialData.duePayments ++ preparedFinancialData.overduePayments

        val periodWithFinancialData: Map[Int, Seq[(Period, Payment)]] = periods.flatMap { period =>
          Map(period -> allPayments.filter(_.period == period).head)
        }.groupBy(_._1.year)

        Ok(view(waypoints, periodWithFinancialData))
      }
  }
}
