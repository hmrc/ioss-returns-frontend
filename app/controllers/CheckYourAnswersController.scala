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

package controllers

import com.google.inject.Inject
import connectors.SaveForLaterConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.audit.{ReturnsAuditModel, SubmissionResult}
import models.etmp.EtmpExclusion
import models.requests.{DataRequest, SaveForLaterRequest}
import models.{ConflictFound, ValidationError}
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, SavedProgressPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.*
import queries.{AllCorrectionPeriodsQuery, TotalAmountVatDueGBPQuery}
import services.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{CardTitle, SummaryList}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionNoPaymentDueSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            salesAtVatRateService: SalesAtVatRateService,
                                            coreVatReturnService: CoreVatReturnService,
                                            auditService: AuditService,
                                            partialReturnPeriodService: PartialReturnPeriodService,
                                            view: CheckYourAnswersView,
                                            saveForLaterConnector: SaveForLaterConnector,
                                            redirectService: RedirectService
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val errors: List[ValidationError] = redirectService.validate(period)

      val businessSummaryListFuture = getBusinessSummaryList(request, waypoints)

      val salesFromEuSummaryListFuture = getSalesFromEuSummaryList(request, waypoints)

      for {
        businessSummaryList <- businessSummaryListFuture
        summaryLists = getAllSummaryLists(request, businessSummaryList, salesFromEuSummaryListFuture, waypoints)
      } yield {

        val containsCorrections = request.userAnswers.get(AllCorrectionPeriodsQuery).isDefined
        
        val (noPaymentDueCountries, totalVatToCountries) = salesAtVatRateService.getVatOwedToCountries(request.userAnswers).partition(vat => vat.totalVat <= 0)
        
        val totalVatOnSales = salesAtVatRateService.getTotalVatOwedAfterCorrections(request.userAnswers)

        val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

        val isFinalReturn: Boolean = partialReturnPeriodService.isFinalReturn(maybeExclusion, period)

        Ok(view(
          waypoints,
          summaryLists,
          request.userAnswers.period,
          totalVatToCountries,
          totalVatOnSales,
          noPaymentDueSummaryList = CorrectionNoPaymentDueSummary.row(noPaymentDueCountries),
          containsCorrections,
          errors.map(_.errorMessage),
          maybeExclusion,
          isFinalReturn
        ))
      }
  }

  def onSubmit(waypoints: Waypoints, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      val userAnswers = request.userAnswers

      val preferredPeriod = userAnswers.period

      val redirectToFirstError = redirectService.getRedirect(waypoints, redirectService.validate(preferredPeriod)).headOption

      (redirectToFirstError, incompletePromptShown) match {
        case (Some(redirect), true) => Redirect(redirect).toFuture
        case (Some(_), false) => Redirect(routes.CheckYourAnswersController.onPageLoad(waypoints)).toFuture
        case _ =>
          coreVatReturnService.submitCoreVatReturn(userAnswers).flatMap { remainingTotalAmountVatDueGBP =>
            auditService.audit(ReturnsAuditModel.build(userAnswers, SubmissionResult.Success))
            userAnswers.set(TotalAmountVatDueGBPQuery, remainingTotalAmountVatDueGBP) match {
              case Failure(exception) =>
                logger.error(s"Couldn't update users answers with remaining owed vat ${exception.getMessage}", exception)
                Future.successful(Redirect(controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad().url))
              case Success(updatedAnswers) =>
                cc.sessionRepository.set(updatedAnswers).map(_ =>
                  Redirect(controllers.submissionResults.routes.SuccessfullySubmittedController.onPageLoad().url)
                )
            }
          }.recoverWith {
            case e: Exception =>
              logger.error(s"Error while submitting VAT return ${e.getMessage}", e)
              auditService.audit(ReturnsAuditModel.build(userAnswers, SubmissionResult.Failure))
              saveUserAnswersOnCoreError(controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad())
          }
      }
  }

  private def getAllSummaryLists(
                                  request: DataRequest[AnyContent],
                                  businessSummaryList: SummaryList,
                                  salesFromEuSummaryList: SummaryList,
                                  waypoints: Waypoints
                                )(implicit messages: Messages): Seq[(Option[String], SummaryList)] =
    if (request.userAnswers.get(CorrectPreviousReturnPage(0)).isDefined) {
      val correctionsSummaryList = SummaryListViewModel(
        rows = Seq(
          CorrectPreviousReturnSummary.row(request.userAnswers, waypoints, CheckYourAnswersPage),
          CorrectionReturnPeriodSummary.getAllRows(request.userAnswers, waypoints, CheckYourAnswersPage)
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

  private def getBusinessSummaryList(request: DataRequest[AnyContent], waypoints: Waypoints)(implicit hc: HeaderCarrier, messages: Messages) = {

    val futureMaybePartialReturnPeriod = partialReturnPeriodService.getPartialReturnPeriod(
      request.iossNumber,
      request.registrationWrapper,
      request.userAnswers.period
    )

    val summaryListFuture = futureMaybePartialReturnPeriod.map { maybePartialReturnPeriod =>
      val period = maybePartialReturnPeriod.getOrElse(request.userAnswers.period)
      val rows = Seq(
        BusinessNameSummary.row(request.registrationWrapper),
        BusinessVRNSummary.row(request.vrn.get), //TODO SCG -> Use the VRN from the request as this matches the old logic. Protect the get
        ReturnPeriodSummary.row(request.userAnswers, waypoints, Some(period))
      ).flatten
      SummaryListViewModel(rows).withCssClass("govuk-summary-card govuk-summary-card__content govuk-!-display-block width-auto")
    }
    summaryListFuture
  }

  private def saveUserAnswersOnCoreError(redirectLocation: Call)(implicit request: DataRequest[AnyContent]): Future[Result] =
    Future.fromTry(request.userAnswers.set(SavedProgressPage, routes.CheckYourAnswersController.onPageLoad().url)).flatMap {
      updatedAnswers =>
        val saveForLateRequest = SaveForLaterRequest(updatedAnswers, request.iossNumber, request.userAnswers.period)

        saveForLaterConnector.submit(saveForLateRequest).flatMap {
          case Right(Some(_)) =>
            for {
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(redirectLocation)
            }
          case Right(None) =>
            logger.error("Unexpected result on submit")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
          case Left(ConflictFound) =>
            Future.successful(Redirect(routes.YourAccountController.onPageLoad()))
          case Left(e) =>
            logger.error(s"Unexpected result on submit: $e")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}