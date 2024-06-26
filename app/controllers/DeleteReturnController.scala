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

package controllers

import connectors.SaveForLaterConnector
import controllers.actions._
import forms.DeleteReturnFormProvider
import models.Period
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.DeleteReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteReturnController @Inject()(
                                        cc: AuthenticatedControllerComponents,
                                        formProvider: DeleteReturnFormProvider,
                                        view: DeleteReturnView,
                                        saveForLaterConnector: SaveForLaterConnector
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>
      Ok(view(form, request.userAnswers.period))
  }

  def onSubmit(period: Period): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, request.userAnswers.period))),
        value =>
          if (value) {
            for {
              _ <- cc.sessionRepository.clear(request.userId)
              _ <- saveForLaterConnector.delete(period)
            } yield Redirect(controllers.routes.YourAccountController.onPageLoad())
          } else {
            Future.successful(Redirect(controllers.routes.ContinueReturnController.onPageLoad(request.userAnswers.period)))
          }
      )
  }
}
