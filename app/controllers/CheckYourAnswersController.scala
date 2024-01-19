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
import models.ValidationError
import models.requests.DataRequest
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import queries.AllCorrectionsQuery
import services._
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{CardTitle, SummaryList}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers._
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            salesAtVatRateService: SalesAtVatRateService,
                                            coreVatReturnService: CoreVatReturnService,
                                            view: CheckYourAnswersView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val errors: List[ValidationError] = Nil // TODO

      val businessSummaryList = getBusinessSummaryList(request, waypoints)

      val salesFromEuSummaryList = getSalesFromEuSummaryList(request, waypoints)

      val containsCorrections = request.userAnswers.get(AllCorrectionsQuery).isDefined

      val (noPaymentDueCountries, totalVatToCountries) = salesAtVatRateService.getVatOwedToCountries(request.userAnswers).partition(vat => vat.totalVat <= 0)

      val totalVatOnSales =
        salesAtVatRateService.getTotalVatOwedAfterCorrections(request.userAnswers)

      val summaryLists = getAllSummaryLists(request, businessSummaryList, salesFromEuSummaryList, waypoints)

        Future(Ok(view(
          waypoints,
          summaryLists,
          request.userAnswers.period,
          totalVatToCountries,
          totalVatOnSales,
          noPaymentDueCountries,
          containsCorrections,
          errors.map(_.errorMessage))))
  }

  def onSubmit(waypoints: Waypoints, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      coreVatReturnService.submitCoreVatReturn(request.userAnswers).map {
        case true => Redirect(controllers.submissionResults.routes.SuccessfullySubmittedController.onPageLoad().url)
        case _ => Redirect(controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad().url)
      }.recover {
        case e: Exception =>
          logger.error(s"Error while submitting VAT return ${e.getMessage}", e)
          Redirect(controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad().url)
      }
  }

  private def getAllSummaryLists(
                                  request: DataRequest[AnyContent],
                                  businessSummaryList: SummaryList,
                                  salesFromEuSummaryList: SummaryList,
                                  waypoints: Waypoints
                                )(implicit messages: Messages) =
    if (request.userAnswers.get(CorrectPreviousReturnPage(0)).isDefined) {
      val correctionsSummaryList = SummaryListViewModel(
        rows = Seq(
          CorrectPreviousReturnSummary.row(request.userAnswers, waypoints, CheckYourAnswersPage),
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
        TotalNetValueOfSalesSummary.row(request.userAnswers, salesAtVatRateService.getTotalNetSales(request.userAnswers), waypoints, CheckYourAnswersPage),
        TotalVatOnSalesSummary.row(request.userAnswers, salesAtVatRateService.getTotalVatOnSales(request.userAnswers), waypoints, CheckYourAnswersPage)
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
    )
  }


}
