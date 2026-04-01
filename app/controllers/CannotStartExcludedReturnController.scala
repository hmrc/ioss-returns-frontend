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

package controllers

import controllers.actions.*
import logging.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.intermediary.DashboardNavigationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.EnrolmentIdentifiers.{findIntermediaryFromEnrolments, findIossFromEnrolments}
import utils.FutureSyntax.FutureOps
import views.html.CannotStartExcludedReturnView

import javax.inject.Inject

class CannotStartExcludedReturnController @Inject()(
                                                     cc: AuthenticatedControllerComponents,
                                                     view: CannotStartExcludedReturnView,
                                                     dashboardNavigationService: DashboardNavigationService
                                                   ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(iossNumber: String): Action[AnyContent] = cc.authAndGetOptionalData(iossNumber).async {
    implicit request =>

      val iossEnrolmentsExist: Boolean = findIossFromEnrolments(request.enrolments).nonEmpty
      val intermediaryEnrolmentsExist: Boolean = findIntermediaryFromEnrolments(request.enrolments).nonEmpty

      val appropriateDashboardUrl: String =
        dashboardNavigationService.getAppropriateDashboardUrl(
          request.isIntermediary, intermediaryEnrolmentsExist, iossEnrolmentsExist
        )

      Ok(view(request.iossNumber, appropriateDashboardUrl)).toFuture
  }
}
