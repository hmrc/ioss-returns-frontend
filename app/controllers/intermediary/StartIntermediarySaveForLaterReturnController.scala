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

package controllers.intermediary

import controllers.actions.*
import models.{ContinueReturn, IntermediarySelectedIossNumber, Period, UserAnswers}
import pages.{ContinueReturnPage, Waypoints}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.IntermediarySelectedIossNumberRepository
import services.saveForLater.SaveForLaterService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartIntermediarySaveForLaterReturnController @Inject()(
                                                               override val messagesApi: MessagesApi,
                                                               cc: AuthenticatedControllerComponents,
                                                               saveForLaterService: SaveForLaterService,
                                                               intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository
                                                             )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, iossNumber: String, period: Period): Action[AnyContent] = cc.authAndIntermediaryRequired(iossNumber).async {
    implicit request =>

      saveForLaterService.getSavedReturnsForClient(iossNumber).flatMap { savedUserAnswers =>
        savedUserAnswers.find(_.period == period).map { answers =>

          val intermediaryNumber = request.intermediaryNumber.get //TODO make "IntermediaryRequiredAction" be a refiner that converts to an int number

          val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(request.userId, intermediaryNumber, iossNumber)

          val userAnswers: UserAnswers = UserAnswers(
            id = request.userId,
            iossNumber = answers.iossNumber,
            period = answers.period,
            data = answers.data,
            lastUpdated = answers.lastUpdated
          )

          for {
            _ <- intermediarySelectedIossNumberRepository.set(intermediarySelectedIossNumber)
            _ <- cc.sessionRepository.set(userAnswers)
          } yield {
            Redirect(ContinueReturnPage.navigate(userAnswers, ContinueReturn.Continue))
          }
        }.getOrElse(Redirect(routes.StartReturnAsIntermediaryController.startReturnAsIntermediary(waypoints, iossNumber)).toFuture)
      }
  }
}
