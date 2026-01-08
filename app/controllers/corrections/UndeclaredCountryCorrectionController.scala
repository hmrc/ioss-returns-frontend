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

package controllers.corrections

import controllers.actions._
import forms.corrections.UndeclaredCountryCorrectionFormProvider
import models.Index
import pages.Waypoints
import pages.corrections.UndeclaredCountryCorrectionPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  extends FrontendBaseController with I18nSupport with CorrectionBaseController {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>

          val period = request.userAnswers.period

          val preparedForm = request.userAnswers.get(UndeclaredCountryCorrectionPage(periodIndex, countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Ok(view(preparedForm, waypoints, period, country, correctionReturnPeriod, periodIndex, countryIndex, request.isIntermediary, request.companyName)).toFuture
        }
      }
  }


  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>

          val period = request.userAnswers.period

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, period, country, correctionReturnPeriod, periodIndex, countryIndex, request.isIntermediary, request.companyName)).toFuture,
            value =>

              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(UndeclaredCountryCorrectionPage(periodIndex, countryIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(UndeclaredCountryCorrectionPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        }
      }
  }
}
