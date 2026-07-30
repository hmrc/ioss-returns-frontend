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
import controllers.actions.*
import pages.{StartReturnPage, Waypoints}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.intermediary.IntermediaryClientService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.InterceptReviewUpdateRegistrationView

import scala.concurrent.ExecutionContext

class InterceptReviewUpdateRegistrationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       frontendAppConfig: FrontendAppConfig,
                                       intermediaryClientService: IntermediaryClientService,
                                       view: InterceptReviewUpdateRegistrationView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def onPageLoad(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.authAndRequireData(iossNumber).async {
    implicit request =>

      val period = request.userAnswers.period
      val changeRegistrationUrl = getChangeRegistrationUrl(request.isIntermediary, iossNumber)
      val continueUrl = StartReturnPage(iossNumber, period, frontendAppConfig).navigate(waypoints, request.userAnswers, request.userAnswers).route.url
      val isIntermediary = request.isIntermediary

      val intermediaryClientName = intermediaryClientService.getClientName(isIntermediary, request.intermediaryNumber, request.iossNumber)

      for {
        clientName <- intermediaryClientName
      } yield {
        Ok(view(waypoints, changeRegistrationUrl, continueUrl, isIntermediary, clientName))
      }
  }

  private def getChangeRegistrationUrl(isIntermediary: Boolean, iossNumber: String): String = {

    if (isIntermediary) {
      s"${frontendAppConfig.changeNetpRegistrationUrl}/$iossNumber"
    } else {
      frontendAppConfig.amendRegistrationUrl
    }
  }
}