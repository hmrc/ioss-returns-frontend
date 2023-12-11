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
import forms.CheckSalesFormProvider
import models.Index
import pages.{CheckSalesPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.RemainingVatRatesFromCountryQuery
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.CheckSalesSummary
import views.html.CheckSalesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckSalesController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: CheckSalesFormProvider,
                                      vatRateService: VatRateService,
                                      view: CheckSalesView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry with GetVatRates {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { vatRates =>

          val period = request.userAnswers.period

          val canAddAnotherVatRate = vatRateService.getRemainingVatRatesForCountry(period, country, vatRates).nonEmpty

          val checkSalesSummary = CheckSalesSummary.rows(request.userAnswers, waypoints, countryIndex)

          val preparedForm = request.userAnswers.get(CheckSalesPage(countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, period, checkSalesSummary, countryIndex, country, canAddAnotherVatRate)).toFuture
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { vatRates =>

          val period = request.userAnswers.period

          val remainingVatRates = vatRateService.getRemainingVatRatesForCountry(period, country, vatRates)

          val canAddAnotherVatRate = remainingVatRates.nonEmpty

          val checkSalesSummary = CheckSalesSummary.rows(request.userAnswers, waypoints, countryIndex)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, period, checkSalesSummary, countryIndex, country, canAddAnotherVatRate)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckSalesPage(countryIndex), value))
                updatedAnswersWithRemainingVatRates <- Future.fromTry(updatedAnswers.set(RemainingVatRatesFromCountryQuery(countryIndex), remainingVatRates))
                _ <- cc.sessionRepository.set(updatedAnswersWithRemainingVatRates)
              } yield Redirect(CheckSalesPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithRemainingVatRates).route)
          )
        }
      }
  }
}
