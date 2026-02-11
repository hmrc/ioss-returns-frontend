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
import connectors.SaveForLaterConnector
import controllers.actions.AuthenticatedControllerComponents
import controllers.routes
import models.{IntermediarySelectedIossNumber, UserAnswers}
import models.requests.OptionalDataRequest
import pages.{EmptyWaypoints, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.IntermediarySelectedIossNumberRepository
import services.{PartialReturnPeriodService, VatReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartReturnAsIntermediaryController @Inject()(
                                                     override val messagesApi: MessagesApi,
                                                     cc: AuthenticatedControllerComponents,
                                                     vatReturnService: VatReturnService,
                                                     intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository,
                                                     saveForLaterConnector: SaveForLaterConnector,
                                                     partialReturnPeriodService: PartialReturnPeriodService,
                                                     config: FrontendAppConfig,
                                                     clock: Clock
                                                   )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def startReturnAsIntermediary(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = (
    cc.authAndIntermediaryRequired(iossNumber) andThen
      cc.getData()).async { implicit request =>

    if (config.intermediaryEnabled) {
      vatReturnService.getOldestDueReturn(iossNumber).flatMap {
        case Some(oldestReturn) =>
          partialReturnPeriodService.getPartialReturnPeriod(request.iossNumber, request.registrationWrapper, oldestReturn.period).flatMap { maybePartialReturnPeriod =>
            val defaultUserAnswers = UserAnswers(
              userId = request.userId,
              iossNumber = iossNumber,
              period = maybePartialReturnPeriod.getOrElse(oldestReturn.period),
              lastUpdated = Instant.now(clock)
            )

            val intermediaryNumber = request.intermediaryNumber.get //TODO make "IntermediaryRequiredAction" be a refiner that converts to an int number

            val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(request.userId, intermediaryNumber, iossNumber)

            for {
              _ <- intermediarySelectedIossNumberRepository.set(intermediarySelectedIossNumber)
              maybeUserAnswers <- getSessionOrSavedUserAnswers(request)
              _ <- cc.sessionRepository.set(maybeUserAnswers.getOrElse(defaultUserAnswers))
            } yield {
              if(oldestReturn.inProgress) {
                Redirect(routes.ContinueReturnController.onPageLoad(oldestReturn.period))
              } else {
                Redirect(routes.StartReturnController.onPageLoad(EmptyWaypoints, oldestReturn.period))
              }
            }
          }
        case _ =>
          val intermediaryNumber = request.intermediaryNumber.get
          val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(request.userId, intermediaryNumber, iossNumber)

          for {
            _ <- intermediarySelectedIossNumberRepository.set(intermediarySelectedIossNumber)
          } yield {
            Redirect(routes.NoReturnsDueController.onPageLoad())
          }
      }
    } else {
      Redirect(routes.NotRegisteredController.onPageLoad()).toFuture
    }
  }

  private def getSessionOrSavedUserAnswers(request: OptionalDataRequest[AnyContent])(implicit hc: HeaderCarrier): Future[Option[UserAnswers]] = {
    for {
      savedForLater <- saveForLaterConnector.get(request.iossNumber)
    } yield {
      val answers = if (request.userAnswers.isEmpty) {
        savedForLater match {
          case Right(Some(answers)) => Some(UserAnswers(request.userId, request.iossNumber, answers.period, answers.data, answers.lastUpdated))
          case _ => None
        }
      } else {
        request.userAnswers
      }
      answers
    }
  }

}
