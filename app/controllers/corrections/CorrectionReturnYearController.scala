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
import forms.CorrectionReturnYearFormProvider
import models.{Index, Period}
import pages.Waypoints
import pages.corrections.CorrectionReturnYearPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.corrections.{AllCorrectionPeriodQuery, DeriveCompletedCorrectionPeriods}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.CorrectionReturnYearView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionReturnYearController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionReturnYearFormProvider,
                                         view: CorrectionReturnYearView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      val period = request.userAnswers.period

      val correctionYears: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

      val form: Form[Period] = formProvider(index, correctionYears)

      val preparedForm = request.userAnswers.get(CorrectionReturnYearPage(index)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, period, correctionYears, index))
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val correctionYears: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List.empty)

      val form: Form[Period] = formProvider(index, correctionYears)

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period, correctionYears, index))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionReturnYearPage(index), value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CorrectionReturnYearPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }
}
