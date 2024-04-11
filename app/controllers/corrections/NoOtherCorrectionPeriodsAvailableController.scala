/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.corrections

import controllers.actions._
import logging.Logging
import models.StandardPeriod
import pages.Waypoints
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.DeriveCompletedCorrectionPeriods
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.NoOtherCorrectionPeriodsAvailableView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class NoOtherCorrectionPeriodsAvailableController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: NoOtherCorrectionPeriodsAvailableView
                                     )(implicit val ec: ExecutionContext)  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = (cc.actionBuilder andThen cc.identify) {
    implicit request =>
      Ok(view(waypoints))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val completedCorrectionPeriods: List[StandardPeriod] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

      if(completedCorrectionPeriods.isEmpty) {
        val cleanup = for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectPreviousReturnPage(0), false))
          _              <- cc.sessionRepository.set(updatedAnswers)
        } yield Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints))

        cleanup.onComplete {
          case Failure(exception) => logger.error(s"Could not perform cleanup: ${exception.getLocalizedMessage} ")
          case _ => ()
        }
        cleanup
      } else {
        Future.successful(Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints)))
      }

  }
}
