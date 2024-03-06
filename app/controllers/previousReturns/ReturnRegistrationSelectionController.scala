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

package controllers.previousReturns

import controllers.actions._
import forms.ReturnRegistrationSelectionFormProvider
import logging.Logging
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SelectedPreviousRegistrationRepository
import services.PreviousRegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.previousReturns.{PreviousRegistration, SelectedPreviousRegistration}
import views.html.previousReturns.ReturnRegistrationSelectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnRegistrationSelectionController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       cc: AuthenticatedControllerComponents,
                                                       formProvider: ReturnRegistrationSelectionFormProvider,
                                                       view: ReturnRegistrationSelectionView,
                                                       previousRegistrationService: PreviousRegistrationService,
                                                       selectedPreviousRegistrationRepository: SelectedPreviousRegistrationRepository
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      for {
        previousRegistrations <- previousRegistrationService.getPreviousRegistrations()
        selectedPreviousRegistration <- selectedPreviousRegistrationRepository.get(request.userId)
      } yield {
        val form: Form[PreviousRegistration] = formProvider(previousRegistrations)

        val preparedForm = selectedPreviousRegistration match {
          case None => form
          case Some(value) => form.fill(value.previousRegistration)
        }

        previousRegistrations match {
          case Nil => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case _ :: Nil => Redirect(controllers.previousReturns.routes.ViewReturnsMultipleRegController.onPageLoad())
          case _ => Ok(view(waypoints, preparedForm, previousRegistrations))
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      previousRegistrationService.getPreviousRegistrations().flatMap { previousRegistrations =>
        val form: Form[PreviousRegistration] = formProvider(previousRegistrations)

        form.bindFromRequest().fold(
          formWithErrors => Future.successful(BadRequest(view(waypoints, formWithErrors, previousRegistrations))),
          value =>
            selectedPreviousRegistrationRepository.set(SelectedPreviousRegistration(request.userId, value)).map { _ =>
              Redirect(controllers.previousReturns.routes.ViewReturnsMultipleRegController.onPageLoad())
            }
        )
      }
  }
}
