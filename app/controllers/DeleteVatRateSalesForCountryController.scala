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
import forms.DeleteVatRateSalesForCountryFormProvider
import logging.Logging
import models.Index
import pages.{DeleteVatRateSalesForCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.VatRateFromCountryQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.DeleteVatRateSalesForCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteVatRateSalesForCountryController @Inject()(
                                                        override val messagesApi: MessagesApi,
                                                        cc: AuthenticatedControllerComponents,
                                                        formProvider: DeleteVatRateSalesForCountryFormProvider,
                                                        view: DeleteVatRateSalesForCountryView
                                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport with GetCountry with GetVatRate with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc


  def onPageLoad(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) {
        country =>

          val vatRate = getVatRate(countryIndex, vatRateIndex, request)

          val form: Form[Boolean] = formProvider(vatRate, country)

          Ok(view(form, waypoints, countryIndex, vatRateIndex, vatRate, country)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      getCountry(waypoints, countryIndex) {
        country =>

          val vatRate: String = getVatRate(countryIndex, vatRateIndex, request)

          val form: Form[Boolean] = formProvider(vatRate, country)

          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, waypoints, countryIndex, vatRateIndex, vatRate, country))),

            value =>
              if (value) {
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.remove(VatRateFromCountryQuery(countryIndex, vatRateIndex)))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(DeleteVatRateSalesForCountryPage(countryIndex, vatRateIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
              } else {
                Redirect(
                  DeleteVatRateSalesForCountryPage(countryIndex, vatRateIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route
                ).toFuture
              }
          )
      }
  }
}
