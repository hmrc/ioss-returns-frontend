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
import forms.VatRatesFromCountryFormProvider
import models.requests.DataRequest
import models.{Country, Index, VatRateFromCountry}
import pages.{JourneyRecoveryPage, SoldToCountryPage, VatRatesFromCountryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.VatRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.VatRatesFromCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatRatesFromCountryController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               cc: AuthenticatedControllerComponents,
                                               formProvider: VatRatesFromCountryFormProvider,
                                               vatRateService: VatRateService,
                                               view: VatRatesFromCountryView
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with GetCountry {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request => {
      val period = request.userAnswers.period

      withCountrySold(waypoints, index) { country =>

        val vatRates = vatRateService.vatRates(period, country)

        val form: Form[List[VatRateFromCountry]] = formProvider(vatRates.toList)
        val answers = request.userAnswers.get(VatRatesFromCountryPage(index))

        val preparedForm = answers match {
          case None => form
          case Some(value) => form.fill(value)
        }

        if (vatRates.size == 1) {
          val onlySingletonSelection = vatRates.toList
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(VatRatesFromCountryPage(index), onlySingletonSelection))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(VatRatesFromCountryPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        } else {
          Future.successful(Ok(view(preparedForm, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRates))))
        }
      }
    }
  }

  def onSubmit(waypoints: Waypoints, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      withCountrySold(waypoints, index) { country =>
        val vatRates = vatRateService.vatRates(period, country)
        val form = formProvider(vatRates.toList)
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRates).toList))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(VatRatesFromCountryPage(index), value))
              _ <- cc.sessionRepository.set(updatedAnswers)
            } yield Redirect(VatRatesFromCountryPage(index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
        )
      }
  }

  private def withCountrySold(waypoints: Waypoints, index: Index)
                             (block: Country => Future[Result])
                             (implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(SoldToCountryPage(index))
      .fold(
        Future.successful(Redirect(JourneyRecoveryPage.route(waypoints).url))
      )(
        block(_)
      )
}
