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
import models.{IntermediarySelectedIossNumber, UserAnswers}
import pages.{EmptyWaypoints, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.IntermediarySelectedIossNumberRepository
import services.VatReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartReturnAsIntermediaryController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     cc: AuthenticatedControllerComponents,
                                                     vatReturnService: VatReturnService,
                                                     intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository,
                                                     config: FrontendAppConfig,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def startReturnAsIntermediary(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = (
    cc.authAndIntermediaryRequired(iossNumber) andThen
      cc.getData()).async { implicit request =>

    // TODO toggle might be better suited in a filter
    if (config.intermediaryEnabled) {
      // TODO check if intermediary has access to ioss number
      vatReturnService.getOldestDueReturn(iossNumber).flatMap {
        case Some(oldestReturn) =>
          // TODO set something to show client in "breadcrumb" type thing
          val defaultUserAnswers = UserAnswers(
            id = request.userId,
            iossNumber = iossNumber,
            period = oldestReturn.period,
            lastUpdated = Instant.now(clock)
          )

          val userAnswers = request.userAnswers.getOrElse(defaultUserAnswers)
          
          val intermediaryNumber = request.intermediaryNumber.get //TODO make "IntermediaryRequiredAction" be a refiner that converts to an int number
          
          val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(request.userId, intermediaryNumber, iossNumber)

          for {
            _ <- intermediarySelectedIossNumberRepository.set(intermediarySelectedIossNumber)
            _ <- cc.sessionRepository.set(userAnswers)
          } yield {
            // TODO should we use waypoints to redirect back to intermediary dashboard at the end? Or maybe external entry?
            Redirect(routes.StartReturnController.onPageLoad(EmptyWaypoints, oldestReturn.period))
          }
        case _ =>
          Redirect(routes.NoReturnsDueController.onPageLoad()).toFuture
      }
    } else {
      Redirect(routes.NotRegisteredController.onPageLoad()).toFuture
    }
  }

}
