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

package controllers

import controllers.actions._
import forms.RemainingVatRateFromCountryFormProvider
import models.Index
import pages.{RemainingVatRateFromCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.AllVatRatesFromCountryQuery
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.RemainingVatRateFromCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemainingVatRateFromCountryController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       cc: AuthenticatedControllerComponents,
                                                       formProvider: RemainingVatRateFromCountryFormProvider,
                                                       vatRateService: VatRateService,
                                                       view: RemainingVatRateFromCountryView
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry with GetVatRates {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(countryIndex) { vatRates =>

          val period = request.userAnswers.period

          val remainingVatRate = vatRateService.getRemainingVatRatesForCountry(period, country, vatRates).head

          val preparedForm = request.userAnswers.get(RemainingVatRateFromCountryPage(countryIndex, vatRateIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, period, countryIndex, vatRateIndex, remainingVatRate.rateForDisplay, country)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(countryIndex) { vatRates =>

          val period = request.userAnswers.period

          val remainingVatRate = vatRateService.getRemainingVatRatesForCountry(period, country, vatRates).head

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, period, countryIndex, vatRateIndex, remainingVatRate.rateForDisplay, country)).toFuture,

            value =>
              if (value) {
                val updatedVatRates = vatRates :+ remainingVatRate
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(RemainingVatRateFromCountryPage(countryIndex, vatRateIndex), value))
                  updatedVatRateAnswersWithFinalVatRate <- Future.fromTry(updatedAnswers.set(AllVatRatesFromCountryQuery(countryIndex), updatedVatRates))
                  _ <- cc.sessionRepository.set(updatedVatRateAnswersWithFinalVatRate)
                } yield {
                  Redirect(RemainingVatRateFromCountryPage(countryIndex, vatRateIndex)
                    .navigate(waypoints, request.userAnswers, updatedVatRateAnswersWithFinalVatRate).route)
                }
              } else {
                Redirect(RemainingVatRateFromCountryPage(countryIndex, vatRateIndex)
                  .navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
              }
          )
        }
      }
  }
}
