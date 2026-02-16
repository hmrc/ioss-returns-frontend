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

import config.FrontendAppConfig
import connectors.UpscanInitiateConnector
import controllers.actions.*
import forms.FileUploadFormProvider
import pages.fileUpload.{FileReferencePage, FileUploadPage, FileUploadedPage}
import pages.Waypoints
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
                                         view: FileUploadView,
                                         upscanInitiateConnector: UpscanInitiateConnector,
                                         appConfig: FrontendAppConfig
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndIntermediaryEnabled().async {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName

      upscanInitiateConnector.initiateV2(
        redirectOnSuccess = Some(appConfig.successEndPointTarget),
        redirectOnError = Some(appConfig.errorEndPointTarget)
      ).flatMap { initiateResponse =>

        val updatedAnswers = request.userAnswers
          .set(FileReferencePage, initiateResponse.fileReference.reference).get

        cc.sessionRepository.set(updatedAnswers).map { _ =>
          Ok(view(
            form = form,
            waypoints = waypoints,
            period = period,
            isIntermediary = isIntermediary,
            companyName = companyName,
            postTarget = initiateResponse.postTarget,
            formFields = initiateResponse.formFields
          ))
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      
      form.bindFromRequest().fold(
        formWithErrors =>
          upscanInitiateConnector.initiateV2(
            redirectOnSuccess = Some(appConfig.successEndPointTarget),
            redirectOnError   = Some(appConfig.errorEndPointTarget)
          ).map { initiateResponse =>
            BadRequest(view(
              formWithErrors,
              waypoints,
              period,
              isIntermediary,
              companyName,
              postTarget = initiateResponse.postTarget,
              formFields = initiateResponse.formFields
            ))
          },

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(FileUploadPage, value))
            cleanup <- Future.fromTry(updatedAnswers.remove(FileUploadedPage))
            _              <- cc.sessionRepository.set(cleanup)
          } yield Redirect(FileUploadPage.navigate(waypoints, request.userAnswers, cleanup).route)
      )
  }
}
