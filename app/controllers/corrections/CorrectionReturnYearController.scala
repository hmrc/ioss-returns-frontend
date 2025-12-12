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

package controllers.corrections

import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import controllers.actions._
import forms.corrections.CorrectionReturnYearFormProvider
import models.{Index, Period}
import pages.Waypoints
import pages.corrections.CorrectionReturnYearPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ObligationsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.ConvertPeriodKey
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionReturnYearView

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnYearController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: CorrectionReturnYearFormProvider,
                                                obligationService: ObligationsService,
                                                view: CorrectionReturnYearView,
                                                clock: Clock
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      val filteredFulfilledObligations = fulfilledObligations.map { obligations =>
        obligations.filter(obligation => !isOlderThanThreeYears(Period.fromKey(obligation.periodKey).paymentDeadline, clock))
      }

      filteredFulfilledObligations.map { obligations =>

        if (obligations.size < 2) {
          Redirect(controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(waypoints, index))
        } else {
          val periodKeys = obligations.map(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey)).distinct

          val form: Form[Int] = formProvider(index, periodKeys)
          val preparedForm = request.userAnswers.get(CorrectionReturnYearPage(index)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, period, utils.ItemsHelper.radioButtonItems(periodKeys), index, request.isIntermediary, request.companyName))
        }
      }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationService.getFulfilledObligations(request.iossNumber)

      val filteredFulfilledObligations = fulfilledObligations.map { obligations =>
        obligations.filter(obligation => !isOlderThanThreeYears(Period.fromKey(obligation.periodKey).paymentDeadline, clock))
      }

      filteredFulfilledObligations.flatMap { obligations =>
        val periodKeys = obligations.map(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey)).distinct

        val form: Form[Int] = formProvider(index, periodKeys)

        form.bindFromRequest().fold(
          formWithErrors =>
            if (obligations.size < 2) {
              Redirect(controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(waypoints, index)).toFuture
            } else {
              BadRequest(view(formWithErrors, waypoints, period, utils.ItemsHelper.radioButtonItems(periodKeys), index, request.isIntermediary, request.companyName)).toFuture
            },

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnYearPage(index), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(CorrectionReturnYearPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
