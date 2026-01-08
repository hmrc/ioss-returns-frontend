/*
 * Copyright 2026 HM Revenue & Customs
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
import forms.corrections.VatPeriodCorrectionsListFormProvider
import models.Period
import models.corrections.PeriodWithCorrections
import models.requests.DataRequest
import pages.Waypoints
import pages.corrections.VatPeriodCorrectionsListPage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllCorrectionPeriodsQuery
import services.VatPeriodCorrectionsService
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.corrections.VatPeriodCorrectionsListSummary
import views.html.corrections.VatPeriodAvailableCorrectionsListView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPeriodCorrectionsListWithFormController @Inject()(
                                                            cc: AuthenticatedControllerComponents,
                                                            formProvider: VatPeriodCorrectionsListFormProvider,
                                                            view: VatPeriodAvailableCorrectionsListView,
                                                            vatPeriodCorrectionsService: VatPeriodCorrectionsService
                                                          )(implicit ec: ExecutionContext) extends FrontendBaseController with VatPeriodCorrections with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      VatPeriodCorrectionsListPage(period, addAnother = false).cleanup(request.userAnswers, cc).flatMap { result =>
        result.fold(
          _ => {
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          },
          _ =>
            findCorrectionsWithCountriesInAnswersWhereNoSubmissionMade(vatPeriodCorrectionsService) {
              val completedCorrectionPeriodsModel: Seq[ListItem] = VatPeriodCorrectionsListSummary.getCompletedRows(waypoints, request.userAnswers)

              val periodWithCorrections: Option[List[PeriodWithCorrections]] = request.userAnswers
                .get(AllCorrectionPeriodsQuery)
              val correctionPeriodsWithNoVatCorrection = vatPeriodCorrectionsService
                .getCorrectionPeriodsWhereCountryVatCorrectionMissesAtLeastOnce(periodWithCorrections)

              Ok(view(form, waypoints, period, completedCorrectionPeriodsModel, correctionPeriodsWithNoVatCorrection, isIntermediary = request.isIntermediary, companyName = request.companyName))
            } orWhenEmpty {
              Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, period))

            }
        )
      }
  }

  def onSubmit(waypoints: Waypoints, period: Period, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request: DataRequest[AnyContent] =>
      val periodWithCorrections: Option[List[PeriodWithCorrections]] = request.userAnswers
        .get(AllCorrectionPeriodsQuery)
      val correctionPeriodsWithNoVatCorrection = vatPeriodCorrectionsService.getCorrectionPeriodsWhereCountryVatCorrectionMissesAtLeastOnce(periodWithCorrections)

      findCorrectionsWithCountriesInAnswersWhereNoSubmissionMade(vatPeriodCorrectionsService) {
        findPeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPage(
          waypoints,
          period,
          incompletePromptShown,
          request,
          correctionPeriodsWithNoVatCorrection
        ) orWhenEmpty {
          val completedCorrectionPeriodsModel: Seq[ListItem] = VatPeriodCorrectionsListSummary.getCompletedRows(waypoints, request.userAnswers)

          form.bindFromRequest().fold(
            formWithErrors => {
              BadRequest(view(formWithErrors, waypoints, period, completedCorrectionPeriodsModel, correctionPeriodsWithNoVatCorrection, isIntermediary = request.isIntermediary, companyName = request.companyName))
            },
            value => {
              Redirect(VatPeriodCorrectionsListPage(period, value).navigate(waypoints, request.userAnswers, request.userAnswers).url)
            }
          )
        }
      } orWhenEmpty {
        Redirect(controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, period))
      }
  }
}
