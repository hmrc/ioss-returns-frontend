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

package controllers.fileUpload

import controllers.actions.*
import forms.FileUploadFormProvider
import pages.{FileUploadPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.fileUpload.FileUploadView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: FileUploadFormProvider,
                                         view: FileUploadView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndIntermediaryEnabled() {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName

      Ok(view(form, waypoints, period, isIntermediary, companyName))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period, isIntermediary, companyName))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(FileUploadPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
