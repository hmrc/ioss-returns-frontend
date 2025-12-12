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

import config.FrontendAppConfig
import controllers.actions.*
import logging.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.CannotStartExcludedReturnView

import javax.inject.Inject
import scala.concurrent.Future

class CannotStartExcludedReturnController @Inject()(
                                                     cc: AuthenticatedControllerComponents,
                                                     frontendAppConfig: FrontendAppConfig,
                                                     view: CannotStartExcludedReturnView
                                                   ) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      val isIntermediary = request.isIntermediary
      val intermediaryDashboardUrl = frontendAppConfig.intermediaryDashboardUrl
      
      Future.successful(Ok(view(isIntermediary, intermediaryDashboardUrl)))
  }
}
