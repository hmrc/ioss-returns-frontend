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
import forms.corrections.VatAmountCorrectionCountryFormProvider
import logging.Logging
import models.requests.DataRequest
import models.{Country, Index, Period}
import pages.corrections.{CorrectionCountryPage, UndeclaredCountryCorrectionPage, VatAmountCorrectionCountryPage}
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.VatAmountCorrectionCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatAmountCorrectionCountryController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      cc: AuthenticatedControllerComponents,
                                                      formProvider: VatAmountCorrectionCountryFormProvider,
                                                      view: VatAmountCorrectionCountryView
                                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] =
    cc.authAndRequireData().async {
      // ToDo: Downstream not ready to check Correction Eligibility yet.
      // ToDo: Enhance the auth when ready
      implicit request =>

        val period = request.userAnswers.period
        //Todo: Change this to correction period when ready

        withCountryCorrected(waypoints, period, countryIndex) {
          country =>
            val form: Form[BigDecimal] = formProvider(country.name)

            val preparedForm = request.userAnswers.get(VatAmountCorrectionCountryPage(period, countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Future.successful((Ok(view(preparedForm, waypoints, period, countryIndex, country))))
        }

    }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] =
    cc.authAndRequireData().async {
      // ToDo: Downstream not ready to check Correction Eligibility yet.
      // ToDo: Enhance the auth when ready
      implicit request => {
        val period = request.userAnswers.period
        //Todo: Change this to correction period when ready
        withCountryCorrected(waypoints, period, countryIndex) {
          country=>
            val form: Form[BigDecimal] = formProvider(country.name)

            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(formWithErrors, waypoints, period, countryIndex, country)).toFuture,
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(VatAmountCorrectionCountryPage(period, countryIndex), value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(VatAmountCorrectionCountryPage(period, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
        }
      }
    }


  private def withCountryCorrected(waypoints: Waypoints, period: Period, index: Index)
                                  (block: Country => Future[Result])
                                  (implicit request: DataRequest[AnyContent]): Future[Result] =
      request.userAnswers.get(CorrectionCountryPage(period, index))
        .fold({
          Future.successful(
            Redirect(
              JourneyRecoveryPage.navigate(
                waypoints,
                request.userAnswers,
                request.userAnswers
              ).route))
        }
        )(country =>
          request.userAnswers.get(UndeclaredCountryCorrectionPage(period, index)) match {
              case Some(true) =>
                block(country)
              case _ =>
                Future.successful(
                  Redirect(
                    UndeclaredCountryCorrectionPage(period, index).route(waypoints)
                  )
                )
          })
}
