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
import forms.corrections.CorrectPreviousReturnFormProvider
import pages.Waypoints
import pages.corrections.CorrectPreviousReturnPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectPreviousReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectPreviousReturnController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectPreviousReturnFormProvider,
                                         obligationService: ObligationsService,
                                         view: CorrectPreviousReturnView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>

        val etmpObligationDetails = obligations.size

        val preparedForm = request.userAnswers.get(CorrectPreviousReturnPage(etmpObligationDetails)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Future.successful(Ok(view(preparedForm, waypoints, period)))
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>

        val etmpObligationDetails = obligations.size

        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, period))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectPreviousReturnPage(etmpObligationDetails), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield {
              Redirect(CorrectPreviousReturnPage(etmpObligationDetails).navigate(waypoints, request.userAnswers, updatedAnswers).route)
            }
        )
      }
  }
}
