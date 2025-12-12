/*
 * Copyright 2025 HM Revenue & Customs
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
import pages.EmptyWaypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartOutstandingReturnController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       vatReturnService: VatReturnService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced.async {
    implicit request =>
      vatReturnService.getOldestDueReturn(request.iossNumber).map {
        case Some(oldestReturn) =>
          Redirect(routes.StartReturnController.onPageLoad(EmptyWaypoints, oldestReturn.period))
        case _ =>
          Redirect(routes.NoReturnsDueController.onPageLoad())
      }
  }
}
