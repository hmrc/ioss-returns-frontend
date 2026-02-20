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

import connectors.FileUploadOutcomeConnector
import controllers.actions.*
import forms.FileUploadedFormProvider
import pages.fileUpload.{FileReferencePage, FileUploadStatusPage, FileUploadedPage}
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.fileUpload.FileUploadedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileUploadedController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: FileUploadedFormProvider,
                                         view: FileUploadedView,
                                         fileUploadOutcomeConnector: FileUploadOutcomeConnector
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndIntermediaryEnabled().async {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      val fileReference = request.userAnswers.get(FileReferencePage)

      fileReference match {
        case Some(ref) =>
          fileUploadOutcomeConnector.getOutcome(ref).flatMap { maybeOutcome =>
            val status = maybeOutcome.map(_.status).getOrElse("UPLOADING")
            val form = formForStatus(status)
            val preparedForm = request.userAnswers.get(FileUploadedPage).fold(form)(form.fill)

            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadStatusPage, status))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Ok(view(preparedForm, waypoints, period, isIntermediary, companyName, maybeOutcome))
            }
          }

        case None =>
          Future.successful(BadRequest("No file reference found in session."))
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      val fileReference = request.userAnswers.get(FileReferencePage)

      fileReference match {
        case Some(ref) =>
          fileUploadOutcomeConnector.getOutcome(ref).flatMap { maybeOutcome =>
            val status = maybeOutcome.map(_.status).getOrElse("UPLOADING")
            val form = formForStatus(status)
            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(view(formWithErrors, waypoints, period, isIntermediary, companyName, maybeOutcome))
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadedPage, value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(FileUploadedPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
          }
        case None =>
          Future.successful(BadRequest("No file reference found in session."))
      }
  }

  private def formForStatus(status: String): Form[Boolean] =
    if (status == "FAILED") formProvider.failedForm else formProvider.successForm
}
