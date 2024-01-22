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
import models.Period
import models.etmp.EtmpVatReturn
import pages.Waypoints
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryList}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist._
import viewmodels.previousReturns._
import views.html.previousReturns.SubmittedReturnForPeriodView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmittedReturnForPeriodController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    cc: AuthenticatedControllerComponents,
                                                    vatReturnConnector: VatReturnConnector,
                                                    view: SubmittedReturnForPeriodView
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      for {
        etmpVatReturn <- vatReturnConnector.get(period)
      } yield {

        val mainSummaryList = getMainSummaryList(etmpVatReturn, period)

        val salesToEuAndNiSummaryList = getSalesToEuAndNiSummaryList(etmpVatReturn)

        val correctionRowsSummaryList = PreviousReturnsCorrectionsSummary.correctionRows(etmpVatReturn)

        val negativeAndZeroBalanceCorrectionCountriesSummaryList =
          PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn)

        val vatOwedSummaryList = getVatOwedSummaryList(etmpVatReturn)

        val totalVatPayable = etmpVatReturn.totalVATAmountDueForAllMSGBP

        Ok(view(
          waypoints,
          period,
          mainSummaryList,
          salesToEuAndNiSummaryList,
          correctionRowsSummaryList,
          negativeAndZeroBalanceCorrectionCountriesSummaryList,
          vatOwedSummaryList,
          totalVatPayable
        ))
      }
  }

  private def getMainSummaryList(etmpVatReturn: EtmpVatReturn, period: Period)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows =
        Seq(
          SubmittedReturnForPeriodSummary.rowVatDeclared(etmpVatReturn),
          SubmittedReturnForPeriodSummary.rowAmountPaid(), // TODO -> Financial call param when created
          SubmittedReturnForPeriodSummary.rowRemainingAmount(), // TODO -> Financial call param when created
          SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(etmpVatReturn),
          SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
          SubmittedReturnForPeriodSummary.rowReturnReference(etmpVatReturn),
          SubmittedReturnForPeriodSummary.rowPaymentReference(etmpVatReturn)
        )
    )
  }

  private def getSalesToEuAndNiSummaryList(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows =
        Seq(
          PreviousReturnsTotalNetValueOfSalesSummary.row(etmpVatReturn),
          PreviousReturnsTotalVatOnSalesSummary.row(etmpVatReturn)
        )
    ).withCard(
      card = Card(
        title = Some(CardTitle(content = HtmlContent(messages("submittedReturnForPeriod.salesToEuNi.title"))))
      )
    )
  }

  private def getVatOwedSummaryList(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows = PreviousReturnsVatOwedSummary.row(etmpVatReturn)
    ).withCard(
      card = Card(
        title = Some(CardTitle(content = if (etmpVatReturn.correctionPreviousVATReturn.isEmpty) {
          HtmlContent(messages("submittedReturnForPeriod.vatOwed.title"))
        } else {
          HtmlContent(messages("submittedReturnForPeriod.vatOwed.titleWithCorrections"))
        }))
      )
    )
  }
}