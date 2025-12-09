/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.VatRatesFromCountryFormProvider
import models.requests.DataRequest
import models.{Index, VatRateFromCountry}
import pages.{CheckSalesPage, RemainingVatRateFromCountryPage, VatRatesFromCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.{AllSalesByCountryQuery, SalesToCountryWithOptionalSales, VatRateWithOptionalSalesFromCountry}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.VatRatesFromCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromCountryController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               formProvider: VatRatesFromCountryFormProvider,
                                               vatRateService: VatRateService,
                                               view: VatRatesFromCountryView
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry with GetVatRates {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request => {
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { currentVatRatesAnswers =>

          val period = request.userAnswers.period

          vatRateService.getRemainingVatRatesForCountry(period, country, currentVatRatesAnswers).flatMap { remainingVatRates =>

            val nextVatRateIndex = Index(currentVatRatesAnswers.vatRatesFromCountry.map(_.size).getOrElse(0))

            val answers = request.userAnswers.get(VatRatesFromCountryPage(countryIndex, nextVatRateIndex))

            val form: Form[List[VatRateFromCountry]] = formProvider(remainingVatRates.toList, isIntermediary)

            val preparedForm = answers match {
              case None => form
              case Some(value) => form.fill(value)
            }

            remainingVatRates.size match {
              case 0 =>
                Redirect(CheckSalesPage(countryIndex).route(waypoints)).toFuture
              case 1 =>
                answers match {
                  case Some(_) =>
                    Redirect(RemainingVatRateFromCountryPage(countryIndex, nextVatRateIndex).route(waypoints)).toFuture
                  case _ =>
                    addVatRateAndRedirect(currentVatRatesAnswers, remainingVatRates.toList, countryIndex, nextVatRateIndex, waypoints)
                }
              case _ =>
                Ok(view(preparedForm, waypoints, period, countryIndex, country, utils.ItemsHelper.checkboxItems(remainingVatRates), request.isIntermediary, request.companyName)).toFuture
            }
          }
        }
      }
    }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { currentVatRatesAnswers =>

          val period = request.userAnswers.period

          vatRateService.getRemainingVatRatesForCountry(period, country, currentVatRatesAnswers).flatMap { remainingVatRates =>

            val form = formProvider(remainingVatRates.toList, isIntermediary)
            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(formWithErrors, waypoints, period, countryIndex, country, utils.ItemsHelper.checkboxItems(remainingVatRates).toList, request.isIntermediary, request.companyName)).toFuture,

              value => {
                val nextVatRateIndex = Index(currentVatRatesAnswers.vatRatesFromCountry.map(_.size).getOrElse(0))
                addVatRateAndRedirect(currentVatRatesAnswers, value, countryIndex, nextVatRateIndex, waypoints)
              }
            )
          }
        }
      }
  }

  private def addVatRateAndRedirect(
                                     currentVatRatesAnswers: SalesToCountryWithOptionalSales,
                                     remainingVatRates: List[VatRateFromCountry],
                                     countryIndex: Index,
                                     nextVatRateIndex: Index,
                                     waypoints: Waypoints
                                   )(implicit request: DataRequest[_]): Future[Result] = {
    val allVatRates = currentVatRatesAnswers.copy(vatRatesFromCountry = {
      currentVatRatesAnswers.vatRatesFromCountry match {
        case Some(currentVatRatesFromCountry) =>
          Some(currentVatRatesFromCountry ++ remainingVatRates.map(VatRateWithOptionalSalesFromCountry.fromVatRateFromCountry))
        case _ =>
          Some(remainingVatRates.map(VatRateWithOptionalSalesFromCountry.fromVatRateFromCountry))
      }
    })

    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AllSalesByCountryQuery(countryIndex), allVatRates))
      _ <- cc.sessionRepository.set(updatedAnswers)
    } yield Redirect(VatRatesFromCountryPage(countryIndex, nextVatRateIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
  }
}
