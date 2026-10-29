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

package controllers.intermediary

import config.FrontendAppConfig
import controllers.actions.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.NoSavedIntermediaryReturnsView

import javax.inject.Inject

class NoSavedIntermediaryReturnsController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      cc: AuthenticatedControllerComponents,
                                                      frontendAppConfig: FrontendAppConfig,
                                                      view: NoSavedIntermediaryReturnsView
                                                    ) extends FrontendBaseController with I18nSupport {
  
  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad: Action[AnyContent] = (cc.actionBuilder andThen cc.identify) {
    implicit request =>

      val redirect: String = frontendAppConfig.intermediaryDashboardUrl

      Ok(view(redirect))
  }
}
