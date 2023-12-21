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

package controllers

import com.google.inject.Inject
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.requests.DataRequest
import models.{Index, Period, ValidationError}
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, DeleteVatRateSalesForCountryPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import queries.AllCorrectionPeriodsQuery
import services._
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, CardTitle, SummaryList}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import viewmodels.checkAnswers._
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            service: SalesAtVatRateService,
                                            view: CheckYourAnswersView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val errors: List[ValidationError] = Nil // TODO

      val businessSummaryList = getBusinessSummaryList(request, waypoints)

      val salesFromEuSummaryList = getSalesFromEuSummaryList(request, waypoints)

      val containsCorrections = request.userAnswers.get(AllCorrectionPeriodsQuery).isDefined

      val (noPaymentDueCountries, totalVatToCountries) = service.getVatOwedToCountries(request.userAnswers).partition(vat => vat.totalVat <= 0)

      val totalVatOnSales =
        service.getTotalVatOwedAfterCorrections(request.userAnswers)

      val summaryLists = getAllSummaryLists(request, businessSummaryList, salesFromEuSummaryList, waypoints)

        Future(Ok(view(
          summaryLists,
          request.userAnswers.period,
          totalVatToCountries,
          totalVatOnSales,
          noPaymentDueCountries,
          containsCorrections,
          errors.map(_.errorMessage))))
  }

  private def getAllSummaryLists(
                                  request: DataRequest[AnyContent],
                                  businessSummaryList: SummaryList,
                                  salesFromEuSummaryList: SummaryList,
                                  waypoints: Waypoints
                                )(implicit messages: Messages) =
    if (request.userAnswers.get(CorrectPreviousReturnPage).isDefined) {
      val correctionsSummaryList = SummaryListViewModel(
        rows = Seq(
          CorrectPreviousReturnSummary.row(request.userAnswers, waypoints),
          CorrectionReturnPeriodSummary.getAllRows(request.userAnswers, waypoints)
        ).flatten
      ).withCard(
        card = Card(
          title = Some(CardTitle(content = HtmlContent(messages("checkYourAnswers.correction.heading")))),
          actions = None
        )
      )
      Seq(
        (None, businessSummaryList),
        (None, salesFromEuSummaryList),
        (None, correctionsSummaryList)
      )
    } else {
      Seq(
        (None, businessSummaryList),
        (Some("checkYourAnswers.sales.heading"), salesFromEuSummaryList)
      )
    }

  private def getSalesFromEuSummaryList(request: DataRequest[AnyContent], waypoints: Waypoints)(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        SoldGoodsSummary.row(request.userAnswers, waypoints, CheckYourAnswersPage),
        TotalNetValueOfSalesSummary.row(request.userAnswers, service.getTotalNetSales(request.userAnswers), waypoints),
        TotalVatOnSalesSummary.row(request.userAnswers, service.getTotalVatOnSales(request.userAnswers), waypoints)
      ).flatten
    ).withCard(
      card = Card(
        title = Some(CardTitle(content = HtmlContent(messages("checkYourAnswers.sales.heading")))),
        actions = None
      )
    )
  }

  private def getBusinessSummaryList(request: DataRequest[AnyContent], waypoints: Waypoints)(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        BusinessNameSummary.row(request.registrationWrapper),
        BusinessVRNSummary.row(request.vrn),
        ReturnPeriodSummary.row(request.userAnswers, waypoints)
      ).flatten
    ).withCard(
      card = Card()
    )
  }

  def onSubmit(period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData().async {
    implicit request =>

      Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
  }

}
