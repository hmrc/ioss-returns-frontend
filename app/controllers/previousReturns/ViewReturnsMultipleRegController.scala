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
import models.requests.DataRequest
import pages.{ReturnRegistrationSelectionPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
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
                                                  view: ViewReturnsMultipleRegView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request: DataRequest[AnyContent] =>
      previousRegistrationService.getPreviousRegistrations().flatMap { previousRegistrations =>
        previousRegistrations match {
          case Nil =>
            println("-- In Nil")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case registration :: Nil =>
            println("-- In one registration")
            okView(waypoints, registration)
          case registrations =>
            println("-- In multiple")
            request.userAnswers.get(ReturnRegistrationSelectionPage) match {
              case Some(selectedRegistration) if registrations.contains(selectedRegistration) =>
                okView(waypoints, selectedRegistration)
              case _ =>
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
        }
      }
  }

  private def okView(waypoints: Waypoints, previousRegistration: PreviousRegistration)(implicit r: Request[_]): Future[Result] = {
    periodWithFinancialDataService.getPeriodWithFinancialData(previousRegistration.iossNumber).map { periodWithFinancialData =>
      Ok(view(waypoints, previousRegistration, periodWithFinancialData))
    }
  }
}
