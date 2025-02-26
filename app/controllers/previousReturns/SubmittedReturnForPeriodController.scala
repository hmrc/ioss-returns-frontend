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

import config.Constants.submittedReturnsPeriodsLimit
import connectors.FinancialDataHttpParser.ChargeResponse
import connectors.VatReturnHttpParser.EtmpVatReturnResponse
import connectors.{FinancialDataConnector, VatReturnConnector}
import controllers.actions.*
import logging.Logging
import models.{PartialReturnPeriod, Period}
import models.etmp.{EtmpExclusion, EtmpExclusionReason, EtmpVatReturn}
import models.requests.OptionalDataRequest
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PreviousRegistrationService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist.*
import viewmodels.previousReturns.*
import views.html.previousReturns.SubmittedReturnForPeriodView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnForPeriodController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    clock: Clock,
                                                    cc: AuthenticatedControllerComponents,
                                                    vatReturnConnector: VatReturnConnector,
                                                    financialDataConnector: FinancialDataConnector,
                                                    previousRegistrationService: PreviousRegistrationService,
                                                    view: SubmittedReturnForPeriodView
                                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndGetOptionalData().async { implicit request =>
    if (isPeriodOlderThanSixYears(period)) {
      Redirect(controllers.routes.NoLongerAbleToViewReturnController.onPageLoad()).toFuture
    } else {
      (for {
        etmpVatReturnResponse <- vatReturnConnector.get(period)
        chargeResponse <- financialDataConnector.getCharge(period)
      } yield onPageLoad(waypoints, period, etmpVatReturnResponse, chargeResponse)).flatten
    }
  }

  def onPageLoadForIossNumber(waypoints: Waypoints, period: Period, iossNumber: String): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      previousRegistrationService.getPreviousRegistrations().flatMap { previousRegistrations =>
        val validIossNumbers: Seq[String] = request.iossNumber :: previousRegistrations.map(_.iossNumber)
        if (validIossNumbers.contains(iossNumber)) {
          (for {
            etmpVatReturnResponse <- vatReturnConnector.getForIossNumber(period, iossNumber)
            chargeResponse <- financialDataConnector.getChargeForIossNumber(period, iossNumber)
          } yield onPageLoad(waypoints, period, etmpVatReturnResponse, chargeResponse)).flatten
        } else {
          Future.successful(Redirect(JourneyRecoveryPage.route(waypoints)))
        }
      }
  }

  private def onPageLoad(waypoints: Waypoints, period: Period, etmpVatReturnResponse: EtmpVatReturnResponse, chargeResponse: ChargeResponse)
                        (implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    (etmpVatReturnResponse, chargeResponse) match {
      case (Right(etmpVatReturn), chargeResponse) =>
        val maybeCharge = chargeResponse.fold(_ => None, charge => charge)

        val determinedPeriod = determineIfPartialPeriod(period, etmpVatReturn)

        val outstandingAmount = maybeCharge.map(_.outstandingAmount)

        val outstanding = outstandingAmount.getOrElse(etmpVatReturn.totalVATAmountPayable)
        val vatDeclared = etmpVatReturn.totalVATAmountDueForAllMSGBP

        val currentReturnExcluded = isCurrentlyExcluded(request.registrationWrapper.registration.exclusions) &&
          hasActiveWindowExpired(Period.fromKey(etmpVatReturn.periodKey).paymentDeadline)

        val displayPayNow = !currentReturnExcluded &&
          (etmpVatReturn.totalVATAmountDueForAllMSGBP > 0 && outstanding > 0)

        val returnIsExcludedAndOutstandingAmount = currentReturnExcluded && (etmpVatReturn.totalVATAmountDueForAllMSGBP > 0 && outstanding > 0)

        val mainSummaryList = SummaryListViewModel(rows = getMainSummaryList(etmpVatReturn, period, outstandingAmount))

        Future.successful(Ok(view(
          waypoints = waypoints,
          period = determinedPeriod,
          mainSummaryList = mainSummaryList,
          salesToEuAndNiSummaryList = getSalesToEuAndNiSummaryList(etmpVatReturn),
          correctionRows = PreviousReturnsCorrectionsSummary.correctionRows(etmpVatReturn),
          negativeAndZeroBalanceCorrectionCountries = PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(etmpVatReturn),
          vatOwedSummaryList = getVatOwedSummaryList(etmpVatReturn),
          totalVatPayable = outstanding,
          vatDeclared = vatDeclared,
          displayPayNow = displayPayNow,
          returnIsExcludedAndOutstandingAmount = returnIsExcludedAndOutstandingAmount
        )))

      case (Left(error), _) =>
        logger.error(s"Unexpected result from api while getting ETMP VAT return: $error")
        Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture

      case _ => Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
    }
  }

  private def getMainSummaryList(
                                  etmpVatReturn: EtmpVatReturn,
                                  period: Period,
                                  outstandingAmount: Option[BigDecimal]
                                )(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      SubmittedReturnForPeriodSummary.rowVatDeclared(etmpVatReturn),
      SubmittedReturnForPeriodSummary.rowRemainingAmount(outstandingAmount),
      SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(etmpVatReturn),
      SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
      SubmittedReturnForPeriodSummary.rowReturnReference(etmpVatReturn),
      SubmittedReturnForPeriodSummary.rowPaymentReference(etmpVatReturn)
    ).flatten
  }

  private def getSalesToEuAndNiSummaryList(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): SummaryList = {
    SummaryListViewModel(
      rows =
        Seq(
          PreviousReturnsTotalNetValueOfSalesSummary.row(etmpVatReturn),
          PreviousReturnsTotalVatOnSalesSummary.row(etmpVatReturn)
        ).flatten
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

  private def isCurrentlyExcluded(exclusions: Seq[EtmpExclusion]): Boolean = {
    val maybeExclusion = exclusions.headOption
    maybeExclusion.exists(_.exclusionReason != EtmpExclusionReason.Reversal)
  }

  private def hasActiveWindowExpired(dueDate: LocalDate): Boolean = {
    val today = LocalDate.now(clock)
    today.isAfter(dueDate.plusYears(3))
  }

  private def isPeriodOlderThanSixYears(period: Period): Boolean = {
    val sixYearsOld = LocalDate.now(clock).minusYears(submittedReturnsPeriodsLimit)
    period.lastDay.isBefore(sixYearsOld)
  }

  private def determineIfPartialPeriod(period: Period, etmpVatReturn: EtmpVatReturn): Period = {
    if (period.firstDay == etmpVatReturn.returnPeriodFrom && period.lastDay == etmpVatReturn.returnPeriodTo) {
      period
    } else {
      PartialReturnPeriod(
        firstDay = etmpVatReturn.returnPeriodFrom,
        lastDay = etmpVatReturn.returnPeriodTo,
        year = period.year,
        month = period.month
      )
    }
  }
}