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
import forms.CorrectionReturnSinglePeriodFormProvider
import models.{Index, Period}

import javax.inject.Inject
import pages.{CorrectionReturnSinglePeriodPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.CorrectionReturnSinglePeriodView

import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnSinglePeriodController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionReturnSinglePeriodFormProvider,
                                         view: CorrectionReturnSinglePeriodView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val preparedForm = request.userAnswers.get(CorrectionReturnSinglePeriodPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, period, index)).toFuture
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period, index))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnSinglePeriodPage(index), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CorrectionReturnSinglePeriodPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
