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
import pages.corrections.{CorrectionCountryPage, UndeclaredCountryCorrectionPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] =
    cc.authAndRequireData().async {
      // ToDo: Downstream not ready to check Correction Eligibility yet.
      // ToDo: Enhance the auth when ready
      implicit request => {

        val period = request.userAnswers.period
        //Todo: Change this to correction period when ready

        withCountryCorrected(period, countryIndex) { country => {
          val preparedForm = request
            .userAnswers.get(UndeclaredCountryCorrectionPage(period, countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Future.successful(Ok(view(preparedForm, waypoints, period, country, countryIndex)))
        }
        }
      }
    }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] =
    cc.authAndRequireData().async {
      // ToDo: Downstream not ready to check Correction Eligibility yet.
      // ToDo: Enhance the auth when ready
      implicit request =>
        val period = request.userAnswers.period
        //Todo: Change this to correction period when ready
        
        withCountryCorrected(period, countryIndex) { country =>
          form.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, waypoints, period, country, countryIndex))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(UndeclaredCountryCorrectionPage(period, countryIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(UndeclaredCountryCorrectionPage(period, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
        }
    }

  private def withCountryCorrected(period: Period, index: Index)
                                  (block: Country => Future[Result])
                                  (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(CorrectionCountryPage(period, index))
      .fold(
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      )(
        block(_)
      )
}
