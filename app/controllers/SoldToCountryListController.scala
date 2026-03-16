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

package controllers

import controllers.actions._
import forms.SoldToCountryListFormProvider
import models.{Country, Index}
import pages.{SoldToCountryListPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.DeriveNumberOfSales
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CompletionChecks
import utils.FutureSyntax.FutureOps
import utils.ItemsHelper.getDerivedItems
import viewmodels.checkAnswers.SoldToCountryListSummary
import views.html.SoldToCountryListView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SoldToCountryListController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: SoldToCountryListFormProvider,
                                             view: SoldToCountryListView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with CompletionChecks with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  // TODO -> URL IOSS NUMBER
  def onPageLoad(waypoints: Waypoints, iossNumber: String): Action[AnyContent] = cc.authAndRequireData(iossNumber).async {
    implicit request =>

      val period = request.userAnswers.period

      getDerivedItems(waypoints, DeriveNumberOfSales) {
        number =>

          val canAddSales = number < Country.euCountriesWithNI.size
          val salesSummary = SoldToCountryListSummary
            .addToListRows(request.userAnswers, waypoints, SoldToCountryListPage(request.iossNumber))

          withCompleteDataAsync[Country](
            data = getCountriesWithIncompleteSales _,
            onFailure = (incomplete: Seq[Country]) => {
              Ok(view(form = form, waypoints = waypoints, request.iossNumber, period = period, salesList = salesSummary, canAddSales = canAddSales, incompleteCountries = incomplete, isIntermediary = request.isIntermediary, companyName = request.companyName)).toFuture
            }) {
            Ok(view(form = form, waypoints = waypoints, request.iossNumber, period = period, salesList = salesSummary, canAddSales = canAddSales, isIntermediary = request.isIntermediary, companyName = request.companyName )).toFuture
          }

      }
  }

  def onSubmit(waypoints: Waypoints, iossNumber: String, incompletePromptShown: Boolean): Action[AnyContent] = cc.authAndRequireData(iossNumber).async {
    implicit request =>

      val period = request.userAnswers.period

      withCompleteDataAsync[Country](
        data = getCountriesWithIncompleteSales _,
        onFailure = (incompleteCountries: Seq[Country]) => {
          if (incompletePromptShown) {
            firstIndexedIncompleteCountrySales(incompleteCountries) match {
              case Some(incompleteCountry) =>
                if (incompleteCountry._1.vatRatesFromCountry.isEmpty) {
                  Redirect(routes.VatRatesFromCountryController.onPageLoad(waypoints, request.iossNumber, Index(incompleteCountry._2))).toFuture
                } else {
                  Redirect(routes.CheckSalesController.onPageLoad(waypoints, request.iossNumber, Index(incompleteCountry._2))).toFuture
                }
              case None =>
                Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
            }
          } else {
            Redirect(routes.SoldToCountryListController.onPageLoad(waypoints, request.iossNumber)).toFuture
          }
        })(getDerivedItems(waypoints, DeriveNumberOfSales) {
        number =>

          val canAddSales = number < Country.euCountriesWithNI.size
          val salesSummary = SoldToCountryListSummary
            .addToListRows(request.userAnswers, waypoints, SoldToCountryListPage(request.iossNumber))

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(form = formWithErrors, waypoints = waypoints, request.iossNumber, period = period, salesList = salesSummary, canAddSales = canAddSales, isIntermediary = request.isIntermediary, companyName = request.companyName)).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(SoldToCountryListPage(request.iossNumber), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(SoldToCountryListPage(request.iossNumber).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      })
  }
}
