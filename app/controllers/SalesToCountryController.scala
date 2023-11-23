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

import controllers.actions._
import forms.SalesToCountryFormProvider
import models.Index
import pages.{SalesToCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.SalesToCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SalesToCountryController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          cc: AuthenticatedControllerComponents,
                                          formProvider: SalesToCountryFormProvider,
                                          view: SalesToCountryView
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with GetCountryAndVatRate with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Int] = formProvider()

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>
      getCountryAndVatRate(waypoints, countryIndex) {
        case (country) =>

        val period = request.userAnswers.period

        val preparedForm = request.userAnswers.get(SalesToCountryPage(countryIndex)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, period, countryIndex, country))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountryAndVatRateAsync(waypoints, countryIndex) {
        case (country) =>

        val period = request.userAnswers.period

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, period, countryIndex, country)).toFuture,

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SalesToCountryPage(countryIndex), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(SalesToCountryPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
