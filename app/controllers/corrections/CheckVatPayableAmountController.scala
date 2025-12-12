/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.corrections

import controllers.actions._
import models.Index
import models.corrections.CorrectionToCountry
import pages.Waypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.corrections.{CountryVatCorrectionSummary, NewVatTotalSummary, PreviousVatTotalSummary}
import viewmodels.govuk.summarylist._
import views.html.corrections.CheckVatPayableAmountView

import javax.inject.Inject

class CheckVatPayableAmountController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 view: CheckVatPayableAmountView
                                               ) extends FrontendBaseController with CompletionChecks with I18nSupport with CorrectionBaseController {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period
      val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
      val selectedCountry = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex))

      (correctionPeriod, selectedCountry) match {
        case (Some(correctionPeriod), Some(country)) =>
          getPreviouslyDeclaredCorrectionAnswers(waypoints, periodIndex, countryIndex) { originalAmount =>

            val summaryList = SummaryListViewModel(
              rows = Seq(
                Some(PreviousVatTotalSummary.row(originalAmount.amount)),
                CountryVatCorrectionSummary.row(request.userAnswers, waypoints, periodIndex, countryIndex),
                NewVatTotalSummary.row(request.userAnswers, periodIndex, countryIndex, originalAmount.amount)
              ).flatten
            )

            withCompleteDataAsync[CorrectionToCountry](
              periodIndex,
              data = getIncompleteCorrections _,
              onFailure = (_: Seq[CorrectionToCountry]) => {
                Ok(view(
                  waypoints,
                  period,
                  summaryList,
                  country,
                  correctionPeriod,
                  periodIndex,
                  countryIndex,
                  countryCorrectionComplete = false,
                  request.isIntermediary,
                  request.companyName
                )).toFuture
              }) {
              Ok(view(waypoints, period, summaryList, country, correctionPeriod, periodIndex, countryIndex, countryCorrectionComplete = true, request.isIntermediary, request.companyName)).toFuture
            }
          }
        case _ =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] =
    cc.authAndGetDataAndCorrectionEligible() {
      implicit request =>
        val incomplete = getIncompleteCorrectionsToCountry(periodIndex, countryIndex)
        if (incomplete.isEmpty) {
          Redirect(controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, periodIndex))
        } else {
          if (incompletePromptShown) {
            Redirect(routes.CorrectionCountryController.onPageLoad(waypoints, periodIndex, countryIndex))
          } else {
            Redirect(routes.CheckVatPayableAmountController.onPageLoad(waypoints, periodIndex, countryIndex))
          }
        }
    }
}
