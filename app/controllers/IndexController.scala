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

import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import controllers.intermediary.routes
import pages.EmptyWaypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class IndexController @Inject()(
                                 cc: AuthenticatedControllerComponents,
                                 appConfig: FrontendAppConfig
                               ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced { implicit request =>

    val iossEnrolmentsExist: Boolean = findIossFromEnrolments(request.enrolments).nonEmpty
    val intermediaryEnrolmentsExist: Boolean = findIntermediaryFromEnrolments(request.enrolments).nonEmpty
    
    (request.isIntermediary, intermediaryEnrolmentsExist, iossEnrolmentsExist) match {
      case (true, true, true) => Redirect(routes.IossOrIntermediaryController.onPageLoad())
      case (true, true, false) => Redirect(appConfig.intermediaryDashboardUrl)
      case _ => Redirect(controllers.routes.YourAccountController.onPageLoad(waypoints = EmptyWaypoints))
    }
  }

  private def findIossFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-ORG")
      .flatMap(_.identifiers.find(id => id.key == "IOSSNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }

  private def findIntermediaryFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-INT")
      .flatMap(_.identifiers.find(id => id.key == "IntNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }
}
