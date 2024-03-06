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
import logging.Logging
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import repositories.SelectedPreviousRegistrationRepository
import services.{PeriodWithFinancialDataService, PreviousRegistrationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.previousReturns.PreviousRegistration
import views.html.previousReturns.ViewReturnsMultipleRegView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewReturnsMultipleRegController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  cc: AuthenticatedControllerComponents,
                                                  periodWithFinancialDataService: PeriodWithFinancialDataService,
                                                  previousRegistrationService: PreviousRegistrationService,
                                                  selectedPreviousRegistrationRepository: SelectedPreviousRegistrationRepository,
                                                  view: ViewReturnsMultipleRegView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      previousRegistrationService.getPreviousRegistrations().flatMap {
        case Nil =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case registration :: Nil =>
          okView(waypoints, registration)
        case registrations =>
          selectedPreviousRegistrationRepository.get(request.userId).flatMap {
            case Some(selectedRegistration) if registrations.contains(selectedRegistration.previousRegistration) =>
              okView(waypoints, selectedRegistration.previousRegistration)
            case _ =>
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      }
  }

  private def okView(waypoints: Waypoints, previousRegistration: PreviousRegistration)(implicit r: Request[_]): Future[Result] = {
    periodWithFinancialDataService.getPeriodWithFinancialData(previousRegistration.iossNumber).map { periodWithFinancialData =>
      Ok(view(waypoints, previousRegistration, periodWithFinancialData))
    }
  }
}
