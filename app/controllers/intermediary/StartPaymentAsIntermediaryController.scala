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
import controllers.actions.AuthenticatedControllerComponents
import controllers.routes
import models.IntermediarySelectedIossNumber
import pages.Waypoints
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.IntermediarySelectedIossNumberRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartPaymentAsIntermediaryController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     cc: AuthenticatedControllerComponents,
                                                     intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository,
                                                     config: FrontendAppConfig
                                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def startPaymentAsIntermediary(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = (
    cc.authAndIntermediaryRequired(iossNumber) andThen
      cc.getData()).async { implicit request =>

    // TODO toggle might be better suited in a filter
    if (config.intermediaryEnabled) {
      val intermediaryNumber = request.intermediaryNumber.get //TODO make "IntermediaryRequiredAction" be a refiner that converts to an int number
      
      val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(request.userId, intermediaryNumber, iossNumber)

      for {
        _ <- intermediarySelectedIossNumberRepository.set(intermediarySelectedIossNumber)
      } yield {
        Redirect(controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad())
      }
    } else {
      Redirect(routes.NotRegisteredController.onPageLoad()).toFuture
    }
  }

}
