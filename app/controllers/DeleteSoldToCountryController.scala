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
import forms.DeleteSoldToCountryFormProvider
import models.Index
import pages.{DeleteSoldToCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.SalesByCountryQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.DeleteSoldToCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteSoldToCountryController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               formProvider: DeleteSoldToCountryFormProvider,
                                               view: DeleteSoldToCountryView
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with AnswerExtractor {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>
      getAnswer(SalesByCountryQuery(countryIndex)) {
        salesToCountryWithOptionalVat =>

          val form: Form[Boolean] = formProvider(salesToCountryWithOptionalVat.country)

          Ok(view(form, waypoints, request.userAnswers.period, countryIndex, salesToCountryWithOptionalVat.country))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getAnswerAsync(SalesByCountryQuery(countryIndex)) {
        salesToCountryWithOptionalVat =>

          val form: Form[Boolean] = formProvider(salesToCountryWithOptionalVat.country)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, request.userAnswers.period, countryIndex, salesToCountryWithOptionalVat.country)).toFuture,

            value =>
              if (value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(SalesByCountryQuery(countryIndex)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteSoldToCountryPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              } else {
                Redirect(DeleteSoldToCountryPage(countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
              }
          )
      }
  }
}
