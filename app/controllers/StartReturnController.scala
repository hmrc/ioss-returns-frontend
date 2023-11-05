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
import forms.StartReturnFormProvider

import javax.inject.Inject
import models.{Mode, NormalMode, Period}
import navigation.Navigator
import pages.StartReturnPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.mvc.Results._
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.StartReturnView

import scala.concurrent.{ExecutionContext, Future}

class StartReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       navigator: Navigator,
                                       formProvider: StartReturnFormProvider,
                                       view: StartReturnView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form = formProvider()

  def onPageLoad(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period) {
    implicit request =>

      Ok(view(form, mode, period))
  }

  def onSubmit(mode: Mode, period: Period): Action[AnyContent] = cc.authAndGetOptionalData(period).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, mode, period)).toFuture,

        value => {
          if (!value) {
            cc.sessionRepository.clear(request.userId)
          }
//          Redirect(routes.SoldGoodsController.onPageLoad(NormalMode, period))
          Redirect("TODO").toFuture
        }
      )
  }
}
