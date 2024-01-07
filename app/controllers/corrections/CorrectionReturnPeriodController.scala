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
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.Index
import pages.Waypoints
import pages.corrections.{CorrectionReturnPeriodPage, CorrectionReturnYearPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConvertPeriodKey
import views.html.corrections.CorrectionReturnPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnPeriodController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionReturnPeriodFormProvider,
                                         obligationService: ObligationsService,
                                         view: CorrectionReturnPeriodView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val openObligations = obligationService.getOpenObligations(request.iossNumber)

      val selectedYear = request.userAnswers.get(CorrectionReturnYearPage(index)).getOrElse(0)

      openObligations.map { obligations =>
        val obligationYears = obligations.filter(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey) == selectedYear)

        val correctionMonths = obligationYears.map { obligation =>
          ConvertPeriodKey.monthNameFromEtmpPeriodKey(obligation.periodKey)
        }.distinct.sorted

        val form: Form[String] = formProvider(index, correctionMonths)

        val preparedForm = request.userAnswers.get(CorrectionReturnPeriodPage(index)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, waypoints, period, utils.ItemsHelper.radioButtonMonthItems(correctionMonths), index))
      }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val openObligations = obligationService.getOpenObligations(request.iossNumber)
      val selectedYear = request.userAnswers.get(CorrectionReturnYearPage(index)).getOrElse(0)

      openObligations.flatMap { obligations =>
        val obligationYears = obligations.filter(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey) == selectedYear)

        val correctionMonths = obligationYears.map { obligation =>
          ConvertPeriodKey.monthNameFromEtmpPeriodKey(obligation.periodKey)
        }.distinct.sorted


        val form: Form[String] = formProvider(index, correctionMonths)

        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, period, utils.ItemsHelper.radioButtonMonthItems(correctionMonths), index))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnPeriodPage(index), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(CorrectionReturnPeriodPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }
}
