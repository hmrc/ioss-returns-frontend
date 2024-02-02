/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.corrections.CorrectionReturnSinglePeriodFormProvider
import models.{Index, Period}
import pages.Waypoints
import pages.corrections.{CorrectionReturnPeriodPage, CorrectionReturnSinglePeriodPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.DeriveCompletedCorrectionPeriods
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConvertPeriodKey
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionReturnSinglePeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnSinglePeriodController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionReturnSinglePeriodFormProvider,
                                         obligationService: ObligationsService,
                                         view: CorrectionReturnSinglePeriodView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>
        val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

        val correctionMonths = obligations.map (obligation => ConvertPeriodKey.periodkeyToPeriod(obligation.periodKey))

        val uncompletedCorrectionPeriods = correctionMonths.diff(completedCorrectionPeriods).distinct

        uncompletedCorrectionPeriods.size match {
          case 0 => Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints)).toFuture
          case 1 => Ok(view(form, waypoints, period, uncompletedCorrectionPeriods.head, index)).toFuture
          case _ => Redirect(
            controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index)
          ).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>

        val completedCorrectionPeriods: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

        val correctionMonths = obligations.map (obligation => ConvertPeriodKey.periodkeyToPeriod(obligation.periodKey))

        val uncompletedCorrectionPeriods = correctionMonths.diff(completedCorrectionPeriods).distinct

        uncompletedCorrectionPeriods.size match {
          case 0 => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case 1 => form.bindFromRequest ().fold(
            formWithErrors => {
              Future.successful(BadRequest(view(formWithErrors, waypoints, period, uncompletedCorrectionPeriods.head, index)))
            },
            value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnSinglePeriodPage(index), value))
                  updatedWithPeriodAnswers <- Future.fromTry(updatedAnswers.set(CorrectionReturnPeriodPage(index), uncompletedCorrectionPeriods.head))
                  _ <- cc.sessionRepository.set(updatedWithPeriodAnswers)
                } yield Redirect(CorrectionReturnSinglePeriodPage(index).navigate(waypoints, request.userAnswers, updatedWithPeriodAnswers).route)

          )
          case _ => Future.successful(Redirect(controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index)))
        }
      }
  }


}
