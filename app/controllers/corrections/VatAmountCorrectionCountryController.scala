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

package controllers.corrections

import controllers.actions._
import forms.corrections.VatAmountCorrectionCountryFormProvider
import logging.Logging
import models.Index
import pages.Waypoints
import pages.corrections.VatAmountCorrectionCountryPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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
  extends FrontendBaseController with I18nSupport with CorrectionBaseController with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>
          getPreviouslyDeclaredCorrectionAnswers(waypoints, periodIndex, countryIndex) { previouslyDeclaredCorrectionAnswers =>

            val period = request.userAnswers.period

            val previouslyDeclaredAmount: BigDecimal = previouslyDeclaredCorrectionAnswers.amount

            val isCountryPreviouslyDeclared: Boolean = previouslyDeclaredCorrectionAnswers.previouslyDeclared

            val form: Form[BigDecimal] = formProvider(country.name, previouslyDeclaredAmount, isIntermediary)

            val preparedForm = request.userAnswers.get(VatAmountCorrectionCountryPage(periodIndex, countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(view(
              preparedForm,
              waypoints,
              period,
              periodIndex,
              correctionReturnPeriod,
              countryIndex,
              country,
              isCountryPreviouslyDeclared,
              previouslyDeclaredAmount,
              request.isIntermediary,
              request.companyName
            )).toFuture
          }
        }
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>
          getPreviouslyDeclaredCorrectionAnswers(waypoints, periodIndex, countryIndex) { previouslyDeclaredCorrectionAnswers =>

            val period = request.userAnswers.period

            val previouslyDeclaredAmount: BigDecimal = previouslyDeclaredCorrectionAnswers.amount

            val isCountryPreviouslyDeclared: Boolean = previouslyDeclaredCorrectionAnswers.previouslyDeclared

            val form: Form[BigDecimal] = formProvider(country.name, previouslyDeclaredAmount, isIntermediary)

            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(view(
                  formWithErrors,
                  waypoints,
                  period,
                  periodIndex,
                  correctionReturnPeriod,
                  countryIndex,
                  country,
                  isCountryPreviouslyDeclared,
                  previouslyDeclaredAmount,
                  request.isIntermediary,
                  request.companyName
                )).toFuture,
              value =>

                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(VatAmountCorrectionCountryPage(periodIndex, countryIndex), value))
                  _ <- cc.sessionRepository.set(updatedAnswers)
                } yield Redirect(VatAmountCorrectionCountryPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
          }
        }
      }
  }
}
