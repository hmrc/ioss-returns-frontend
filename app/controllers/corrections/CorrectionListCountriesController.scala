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
import forms.corrections.CorrectionListCountriesFormProvider
import models.{Country, Index}
import pages.Waypoints
import pages.corrections.CorrectionListCountriesPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.corrections.CorrectionListCountriesSummary
import views.html.corrections.CorrectionListCountriesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionListCountriesController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionListCountriesFormProvider,
                                         view: CorrectionListCountriesView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with VatCorrectionBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible() {
    implicit request =>

      val period = request.userAnswers.period

        getNumberOfCorrections(periodIndex) {
          (number, correctionPeriod) =>

            val canAddCountries = number < Country.euCountriesWithNI.size
            val list = CorrectionListCountriesSummary
              .addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))

           Ok(view(form, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries, Seq.empty))
        }

  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      getNumberOfCorrectionsAsync(periodIndex) {
        (number, correctionPeriod) =>
          val canAddCountries = number < Country.euCountriesWithNI.size
          val list = CorrectionListCountriesSummary
            .addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries, Seq.empty))),

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionListCountriesPage(periodIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(CorrectionListCountriesPage(periodIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route))

      }
  }


}
