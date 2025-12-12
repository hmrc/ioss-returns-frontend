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

import controllers.actions._
import forms.corrections.RemovePeriodCorrectionFormProvider
import models.Index
import pages.Waypoints
import pages.corrections.RemovePeriodCorrectionPage
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AllCorrectionPeriodsQuery, CorrectionPeriodQuery, DeriveNumberOfCorrectionPeriods}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.RemovePeriodCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemovePeriodCorrectionController @Inject()(
                                                  cc: AuthenticatedControllerComponents,
                                                  formProvider: RemovePeriodCorrectionFormProvider,
                                                  view: RemovePeriodCorrectionView
                                                )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CorrectionBaseController {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionPeriod =>

        val preparedForm: Form[Boolean] = formProvider(correctionPeriod)

        Ok(view(preparedForm, waypoints, request.userAnswers.period, periodIndex, correctionPeriod, request.isIntermediary, request.companyName)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionPeriod =>

        val form: Form[Boolean] = formProvider(correctionPeriod)

        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, request.userAnswers.period, periodIndex, correctionPeriod, request.isIntermediary, request.companyName))),

          value =>
            if (value) {
              Future.fromTry(request.userAnswers.remove(CorrectionPeriodQuery(periodIndex))).flatMap {
                updatedAnswers =>
                  if (updatedAnswers.get(DeriveNumberOfCorrectionPeriods).getOrElse(0) == 0) {
                    for {
                      answersWithRemovedPeriods <- Future.fromTry(updatedAnswers.remove(AllCorrectionPeriodsQuery))
                      _ <- cc.sessionRepository.set(answersWithRemovedPeriods)
                    } yield {
                      Redirect(RemovePeriodCorrectionPage(periodIndex).navigate(waypoints, request.userAnswers, answersWithRemovedPeriods).route)
                    }
                  } else {
                    for {
                      _ <- cc.sessionRepository.set(updatedAnswers)
                    } yield {
                      Redirect(RemovePeriodCorrectionPage(periodIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
                    }
                  }
              }
            } else {
              Future.successful(Redirect(RemovePeriodCorrectionPage(periodIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route))
            }
        )
      }
  }
}
