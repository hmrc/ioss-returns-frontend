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
import forms.corrections.VatPayableForCountryFormProvider
import models.requests.DataRequest
import models.{Country, Index, Period}
import pages.Waypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage, VatPayableForCountryPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.corrections.VatPayableForCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPayableForCountryController @Inject()(
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: VatPayableForCountryFormProvider,
                                                view: VatPayableForCountryView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      withAmountPeriodAndCountryCorrected(periodIndex, countryIndex) { data => {
        val (selectedCountry, correctionPeriod, correctionAmount) = data
          val form = formProvider(selectedCountry, correctionAmount)
          val preparedForm = request.userAnswers.get(VatPayableForCountryPage(periodIndex, countryIndex)) match {
            case None => form
            case Some(value) => form.fill(value)
          }
          Future.successful(Ok(view(preparedForm, waypoints, periodIndex, countryIndex, selectedCountry, correctionPeriod, correctionAmount)))
        }
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
      implicit request =>
        withAmountPeriodAndCountryCorrected(periodIndex, countryIndex) { data => {
          val (selectedCountry, correctionPeriod, correctionAmount) = data
            val form = formProvider(selectedCountry, correctionAmount)
            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, waypoints, periodIndex, countryIndex, selectedCountry, correctionPeriod, correctionAmount))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(VatPayableForCountryPage(periodIndex, countryIndex), value))
                } yield Redirect(VatPayableForCountryPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
          }
        }
    }

  private def withAmountPeriodAndCountryCorrected(periodIndex: Index, countryIndex: Index)
                                                 (block: ((Country, Period, BigDecimal)) => Future[Result])
                                                 (
                                                   implicit request: DataRequest[AnyContent]
                                                 ): Future[Result] = {
    val correctionPeriod = request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex))
    correctionPeriod match {
      case Some(correctionPeriod) =>
        val result: Option[(Country, Period, BigDecimal)] = for {
          selectedCountry <- request.userAnswers.get(CorrectionCountryPage (periodIndex, countryIndex))
          correctionAmount <- request.userAnswers.get(VatAmountCorrectionCountryPage (periodIndex, countryIndex))
        } yield (selectedCountry, correctionPeriod, correctionAmount)

        result
          .fold (
            Future.successful (Redirect (controllers.routes.JourneyRecoveryController.onPageLoad () ) )
          ) (block (_) )
      case _ =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
