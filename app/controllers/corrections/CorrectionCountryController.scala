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

package controllers.corrections

import controllers.actions._
import forms.corrections.CorrectionCountryFormProvider
import models.Index
import pages.Waypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllCorrectionCountriesQuery
import services.CorrectionsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionCountryController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: CorrectionCountryFormProvider,
                                             correctionsService: CorrectionsService,
                                             view: CorrectionCountryView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CorrectionBaseController {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      val period = request.userAnswers.period

      val form = formProvider(
        countryIndex,
        request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
          .getOrElse(Seq.empty)
          .map(_.correctionCountry)
      )

      val preparedForm = request.userAnswers.get(CorrectionCountryPage(periodIndex, countryIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex)) match {
        case Some(correctionPeriod) => Ok(view(preparedForm, waypoints, period, periodIndex, correctionPeriod, countryIndex))
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url)
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      //      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>

      val period = request.userAnswers.period

      val form = formProvider(
        countryIndex,
        request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
          .getOrElse(Seq.empty)
          .map(_.correctionCountry)
      )

      form.bindFromRequest().fold(
        formWithErrors =>
          request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex)) match {
            case Some(correctionPeriod) => BadRequest(view(formWithErrors, waypoints, period, periodIndex, correctionPeriod, countryIndex)).toFuture
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url).toFuture
          },
        value =>

          // TODO -> Call correctionsService.getAccumulativeVatForCountryTotalAmount(correctionReturnPeriod, period, value) and save to query?

          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionCountryPage(periodIndex, countryIndex), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CorrectionCountryPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
  //  }
}
