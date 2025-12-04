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
import forms.CheckSalesFormProvider
import models.Index
import pages.{CheckSalesPage, SalesToCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{RemainingVatRatesFromCountryQuery, VatRateWithOptionalSalesFromCountry}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
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
                                    )(implicit ec: ExecutionContext)
  extends FrontendBaseController with CompletionChecks with I18nSupport with GetCountry with GetVatRates {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      println("???")
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { vatRates =>

          val period = request.userAnswers.period

          vatRateService.getRemainingVatRatesForCountry(period, country, vatRates).flatMap { otherVatRates =>

            val canAddAnotherVatRate = otherVatRates.nonEmpty

            val checkSalesSummary = CheckSalesSummary.rows(request.userAnswers, waypoints, countryIndex)

            withCompleteDataAsync[VatRateWithOptionalSalesFromCountry](
              countryIndex,
              data = getIncompleteVatRateAndSales _,
              onFailure = (incomplete: Seq[VatRateWithOptionalSalesFromCountry]) => {
                Ok(view(
                  form = form,
                  waypoints = waypoints,
                  period = period,
                  checkSalesSummaryLists = checkSalesSummary,
                  countryIndex = countryIndex,
                  country = country,
                  canAddAnotherVatRate = canAddAnotherVatRate,
                  incompleteSales = incomplete,
                  companyName = request.companyName,
                  isIntermediary = request.isIntermediary)).toFuture
              }) {
              Ok(view(
                form = form,
                waypoints = waypoints,
                period = period,
                checkSalesSummaryLists = checkSalesSummary,
                countryIndex = countryIndex,
                country = country,
                canAddAnotherVatRate = canAddAnotherVatRate,
                companyName = request.companyName,
                isIntermediary = request.isIntermediary)).toFuture
            }
          }
        }
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) { country =>
        getAllVatRatesFromCountry(waypoints, countryIndex) { vatRates =>
          val period = request.userAnswers.period

          vatRateService.getRemainingVatRatesForCountry(period, country, vatRates).flatMap { remainingVatRates =>

            val vatRateIndex = Index(vatRates.vatRatesFromCountry.map(_.size).getOrElse(0) - 1)

            val canAddAnotherVatRate = remainingVatRates.nonEmpty

            val checkSalesSummary = CheckSalesSummary.rows(request.userAnswers, waypoints, countryIndex)

            val salesToCountry = request.userAnswers.get(SalesToCountryPage(countryIndex, vatRateIndex))

            withCompleteDataAsync[VatRateWithOptionalSalesFromCountry](
              countryIndex,
              data = getIncompleteVatRateAndSales _,
              onFailure = (_: Seq[VatRateWithOptionalSalesFromCountry]) => {
                if (incompletePromptShown) {
                  salesToCountry match {
                    case Some(_) =>
                      Redirect(routes.VatOnSalesController.onPageLoad(waypoints, countryIndex, vatRateIndex)).toFuture
                    case None =>
                      Redirect(routes.SalesToCountryController.onPageLoad(waypoints, countryIndex, vatRateIndex)).toFuture
                  }
                } else {
                  Redirect(routes.CheckSalesController.onPageLoad(waypoints, countryIndex)).toFuture
                }
              }) {
              form.bindFromRequest().fold(
                formWithErrors =>
                  BadRequest(view(formWithErrors, waypoints, period, checkSalesSummary, countryIndex, country, canAddAnotherVatRate, Seq.empty, request.companyName, request.isIntermediary)).toFuture,

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CheckSalesPage(countryIndex), value))
                    updatedAnswersWithRemainingVatRates <- Future.fromTry(
                      updatedAnswers.set(RemainingVatRatesFromCountryQuery(countryIndex), remainingVatRates)
                    )
                    _ <- cc.sessionRepository.set(updatedAnswersWithRemainingVatRates)
                  } yield Redirect(CheckSalesPage(countryIndex).navigate(waypoints, request.userAnswers, updatedAnswersWithRemainingVatRates).route)
              )
            }
          }
        }
      }
  }
}
