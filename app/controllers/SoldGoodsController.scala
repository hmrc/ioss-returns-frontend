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
import forms.SoldGoodsFormProvider
import models.Period
import pages.{SoldGoodsPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SoldGoodsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SoldGoodsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: SoldGoodsFormProvider,
                                         view: SoldGoodsView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form = formProvider()

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndRequireData(period) {
    implicit request =>

      val preparedForm = request.userAnswers.get(SoldGoodsPage(period)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, period))
  }

  def onSubmit(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndRequireData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(SoldGoodsPage(period), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(SoldGoodsPage(period).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
