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
import forms.VatOnSalesFormProvider
import models.{Index, VatOnSales}
import pages.{VatOnSalesPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.VatOnSalesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatOnSalesController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      cc: AuthenticatedControllerComponents,
                                      formProvider: VatOnSalesFormProvider,
                                      vatRateService: VatRateService,
                                      view: VatOnSalesView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with SalesFromCountryBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      getCountryVatRateAndNetSales(countryIndex, vatRateIndex) {
        case (country, vatRateFromCountry, netSales) =>
          val form: Form[VatOnSales] = formProvider(vatRateFromCountry, netSales)

          val period = request.userAnswers.period
          val standardVat = vatRateService.standardVatOnSales(netSales, vatRateFromCountry)

          val preparedForm = request.userAnswers.get(VatOnSalesPage(countryIndex, vatRateIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(preparedForm, waypoints, period, countryIndex, vatRateIndex, country, vatRateFromCountry, netSales, standardVat, request.isIntermediary, request.registrationWrapper.getCompanyName()))
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period
      getCountryVatRateAndNetSalesAsync(countryIndex, vatRateIndex) {
        case (country, vatRate, netSales) =>

          val form = formProvider(vatRate, netSales)
          val standardVat = vatRateService.standardVatOnSales(netSales, vatRate)

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, period, countryIndex, vatRateIndex, country, vatRate, netSales, standardVat, request.isIntermediary, request.registrationWrapper.getCompanyName())).toFuture,

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(VatOnSalesPage(countryIndex, vatRateIndex), value))
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield Redirect(VatOnSalesPage(countryIndex, vatRateIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
          )
      }
  }
}
