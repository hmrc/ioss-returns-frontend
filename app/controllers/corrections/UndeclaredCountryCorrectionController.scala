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
import forms.corrections.UndeclaredCountryCorrectionFormProvider
import models.requests.DataRequest
import models.{Country, Index, Period}
import pages.Waypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, UndeclaredCountryCorrectionPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.UndeclaredCountryCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UndeclaredCountryCorrectionController @Inject()(
                                                       cc: AuthenticatedControllerComponents,
                                                       formProvider: UndeclaredCountryCorrectionFormProvider,
                                                       view: UndeclaredCountryCorrectionView
                                                     )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
      implicit request => {

        val period = request.userAnswers.period
        val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))

        correctionPeriod match {
          case Some(correctionPeriod) =>
            withCountryCorrected(period, periodIndex, countryIndex) {
              country => {
                val preparedForm = request
                  .userAnswers.get(UndeclaredCountryCorrectionPage(periodIndex, countryIndex)) match {
                  case None => form
                  case Some (value) => form.fill (value)
                }
                Future.successful(Ok(view(preparedForm, waypoints, period, country, correctionPeriod, periodIndex, countryIndex)))
              }
            }
          case _ =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url).toFuture
        }
      }
    }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
      implicit request =>
        val period = request.userAnswers.period
        val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))

        correctionPeriod match {
          case Some(correctionPeriod) =>
            withCountryCorrected (period, periodIndex, countryIndex) {
              country =>
                form.bindFromRequest ().fold (
                  formWithErrors => Future.successful(BadRequest(
                    view(formWithErrors, waypoints, period, country, correctionPeriod, periodIndex, countryIndex))),
                  value =>
                    for {
                      updatedAnswers <- Future.fromTry (
                        request.userAnswers.set(UndeclaredCountryCorrectionPage (periodIndex, countryIndex), value))
                      _ <- cc.sessionRepository.set(updatedAnswers)
                    } yield Redirect(UndeclaredCountryCorrectionPage(periodIndex, countryIndex).navigate (waypoints, request.userAnswers, updatedAnswers).route)
                )
            }
          case _ =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  private def withCountryCorrected(period: Period, periodIndex: Index, index: Index)
                                  (block: Country => Future[Result])
                                  (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(CorrectionCountryPage(periodIndex, index))
      .fold(
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      )(
        block(_)
      )
}
