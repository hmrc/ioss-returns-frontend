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

import cats.data.Validated.{Invalid, Valid}
import com.google.inject.Inject
import connectors.{SaveForLaterConnector, VatReturnConnector}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.requests.DataRequest
import models.{NormalMode, Period}
import pages.{CheckAnswersPage, CheckYourAnswersPage, Waypoints}
import pages.CheckYourAnswersPage.waypoint
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import repositories.CachedVatReturnRepository
import services.corrections.CorrectionService
import services.exclusions.ExclusionService
import services.{AuditService, EmailService, RedirectService, SalesAtVatRateService, VatReturnService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax._
import viewmodels.checkAnswers.{BusinessNameSummary, BusinessVRNSummary, ReturnPeriodSummary, SoldGoodsFromEuSummary, SoldGoodsSummary, TotalNetValueOfSalesSummary, TotalVatOnSalesSummary}
import viewmodels.checkAnswers.corrections.CorrectPreviousReturnSummary
import viewmodels.govuk.summarylist._
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            cc: AuthenticatedControllerComponents,
                                            service: SalesAtVatRateService,
                                            exclusionService: ExclusionService,
                                            view: CheckYourAnswersView,
                                            vatReturnService: VatReturnService,
                                            redirectService: RedirectService,
                                            correctionService: CorrectionService,
                                            auditService: AuditService,
                                            emailService: EmailService,
                                            vatReturnConnector: VatReturnConnector,
                                            cachedVatReturnRepository: CachedVatReturnRepository,
                                            saveForLaterConnector: SaveForLaterConnector
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val errors = redirectService.validate(period)

      val businessSummaryList = getBusinessSummaryList(request)

      val salesFromEuSummaryList = getSalesFromEuSummaryList(request)

      val containsCorrections = request.userAnswers.get(AllCorrectionPeriodsQuery).isDefined

      val totalVatToCountries =
        service.getVatOwedToEuCountries(request.userAnswers).filter(vat => vat.totalVat > 0)
      val noPaymentDueCountries =
        service.getVatOwedToEuCountries(request.userAnswers).filter(vat => vat.totalVat <= 0)

      val totalVatOnSales =
        service.getTotalVatOwedAfterCorrections(request.userAnswers)

      val summaryLists = getAllSummaryLists(request, businessSummaryList, salesFromNiSummaryList, salesFromEuSummaryList)

      for {
        currentReturnIsFinal <- exclusionService.currentReturnIsFinal(request.registration, request.userAnswers.period)
      } yield {
        Ok(view(
          summaryLists,
          request.userAnswers.period,
          totalVatToCountries,
          totalVatOnSales,
          noPaymentDueCountries,
          containsCorrections,
          errors.map(_.errorMessage),
          request.registration.excludedTrader,
          currentReturnIsFinal
        ))
      }
  }

  private def getAllSummaryLists(
                                  request: DataRequest[AnyContent],
                                  businessSummaryList: SummaryList,
                                  salesFromNiSummaryList: SummaryList,
                                  salesFromEuSummaryList: SummaryList
                                )(implicit messages: Messages) =
    if (request.userAnswers.get(CorrectPreviousReturnPage).isDefined) {
      val correctionsSummaryList = SummaryListViewModel(
        rows = Seq(
          CorrectPreviousReturnSummary.row(request.userAnswers),
          CorrectionReturnPeriodSummary.getAllRows(request.userAnswers)
        ).flatten
      ).withCssClass("govuk-!-margin-bottom-9")
      Seq(
        (None, businessSummaryList),
        (Some("checkYourAnswers.salesFromNi.heading"), salesFromNiSummaryList),
        (Some("checkYourAnswers.salesFromEU.heading"), salesFromEuSummaryList),
        (Some("checkYourAnswers.corrections.heading"), correctionsSummaryList)
      )
    } else {
      Seq(
        (None, businessSummaryList),
        (Some("checkYourAnswers.salesFromNi.heading"), salesFromNiSummaryList),
        (Some("checkYourAnswers.salesFromEU.heading"), salesFromEuSummaryList)
      )
    }

  private def getSalesFromEuSummaryList(request: DataRequest[AnyContent], waypoints: Waypoints)(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        SoldGoodsSummary.row(request.userAnswers, waypoints, CheckYourAnswersPage),
        TotalNetValueOfSalesSummary.row(request.userAnswers, service.getEuTotalNetSales(request.userAnswers), waypoints),
        TotalVatOnSalesSummary.row(request.userAnswers, service.getEuTotalVatOnSales(request.userAnswers), waypoints)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
  }

  private def getBusinessSummaryList(request: DataRequest[AnyContent])(implicit messages: Messages) = {
    SummaryListViewModel(
      rows = Seq(
        BusinessNameSummary.row(request.registrationWrapper),
        BusinessVRNSummary.row(request.vrn),
        ReturnPeriodSummary.row(request.userAnswers)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
  }

  def onSubmit(period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndGetData(period).async {
    implicit request =>

      val redirectToFirstError = redirectService.getRedirect(redirectService.validate(period), period).headOption

      (redirectToFirstError, incompletePromptShown) match {
        case (Some(redirect), true) => Future.successful(Redirect(redirect))
        case (Some(_), false) => Future.successful(Redirect(routes.CheckYourAnswersController.onPageLoad(period)))
        case _ =>
          val validatedVatReturnRequest =
            vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration)

          val validatedCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage).map(_ =>
            correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, request.registration.commencementDate))

          (validatedVatReturnRequest, validatedCorrectionRequest) match {
            case (Valid(vatReturnRequest), Some(Valid(correctionRequest))) =>
              submitReturn(vatReturnRequest, Option(correctionRequest), period)
            case (Valid(vatReturnRequest), None) =>
              submitReturn(vatReturnRequest, None, period)
            case (Invalid(vatReturnErrors), Some(Invalid(correctionErrors))) =>
              val errors = vatReturnErrors ++ correctionErrors
              val errorList = errors.toChain.toList
              val errorMessages = errorList.map(_.errorMessage).mkString("\n")
              logger.error(s"Unable to create a vat return and correction request from user answers: $errorMessages")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
            case (Invalid(errors), _) =>
              val errorList = errors.toChain.toList
              val errorMessages = errorList.map(_.errorMessage).mkString("\n")
              logger.error(s"Unable to create a vat return request from user answers: $errorMessages")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
            case (_, Some(Invalid(errors))) =>
              val errorList = errors.toChain.toList
              val errorMessages = errorList.map(_.errorMessage).mkString("\n")
              logger.error(s"Unable to create a Corrections request from user answers: $errorMessages")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
          }
      }
  }

}

///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import com.google.inject.Inject
//import controllers.actions.AuthenticatedControllerComponents
//import models.CheckMode
//import pages.{CheckYourAnswersPage, EmptyWaypoints, Waypoint}
//import play.api.i18n.{I18nSupport, MessagesApi}
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
//import viewmodels.govuk.summarylist._
//import views.html.CheckYourAnswersView
//
//class CheckYourAnswersController @Inject()(
//                                            override val messagesApi: MessagesApi,
//                                            cc: AuthenticatedControllerComponents,
//                                            view: CheckYourAnswersView
//                                          ) extends FrontendBaseController with I18nSupport {
//
//  protected val controllerComponents: MessagesControllerComponents = cc
//
//  def onPageLoad(): Action[AnyContent] = cc.authAndRequireData() {
//    implicit request =>
//
//      val thisPage = CheckYourAnswersPage
//
//      val waypoints = EmptyWaypoints.setNextWaypoint(Waypoint(thisPage, CheckMode, CheckYourAnswersPage.urlFragment))
//
//      val list = SummaryListViewModel(
//        rows = Seq.empty
//      )
//
//      Ok(view(list))
//  }
//}
