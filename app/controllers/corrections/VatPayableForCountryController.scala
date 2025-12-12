/*
 * Copyright 2025 HM Revenue & Customs
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
import models.Index
import pages.Waypoints
import pages.corrections.{VatAmountCorrectionCountryPage, VatPayableForCountryPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.VatPayableForCountryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPayableForCountryController @Inject()(
                                                cc: AuthenticatedControllerComponents,
                                                formProvider: VatPayableForCountryFormProvider,
                                                view: VatPayableForCountryView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CorrectionBaseController {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>

      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>
          getPreviouslyDeclaredCorrectionAnswers(waypoints, periodIndex, countryIndex) { previouslyDeclaredCorrectionAnswers =>

            val correctionAmount = request.userAnswers.get(
              VatAmountCorrectionCountryPage(periodIndex, countryIndex)
            ).getOrElse(BigDecimal(0))

            val calculatedCorrectionAmount = if (previouslyDeclaredCorrectionAnswers.previouslyDeclared) {
              previouslyDeclaredCorrectionAnswers.amount + correctionAmount
            } else {
              correctionAmount
            }

            val form = formProvider(country, calculatedCorrectionAmount)
            val preparedForm = request.userAnswers.get(VatPayableForCountryPage(periodIndex, countryIndex)) match {
              case None => form
              case Some(value) => form.fill(value)
            }
            Ok(
              view(
                preparedForm,
                waypoints,
                request.userAnswers.period,
                periodIndex,
                countryIndex,
                country,
                correctionReturnPeriod,
                calculatedCorrectionAmount,
                request.isIntermediary,
                request.companyName
              )
            ).toFuture
          }
        }
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndGetDataAndCorrectionEligible().async {
    implicit request =>
      getCorrectionReturnPeriod(waypoints, periodIndex) { correctionReturnPeriod =>
        getCountry(waypoints, periodIndex, countryIndex) { country =>
          getPreviouslyDeclaredCorrectionAnswers(waypoints, periodIndex, countryIndex) { previouslyDeclaredCorrectionAnswers =>

            val correctionAmount = request.userAnswers.get(
              VatAmountCorrectionCountryPage(periodIndex, countryIndex)
            ).getOrElse(BigDecimal(0))

            val calculatedCorrectionAmount = if (previouslyDeclaredCorrectionAnswers.previouslyDeclared) {
              previouslyDeclaredCorrectionAnswers.amount + correctionAmount
            } else {
              correctionAmount
            }

            val form = formProvider(country, calculatedCorrectionAmount)
            form.bindFromRequest().fold(
              formWithErrors =>
                BadRequest(
                  view(
                    formWithErrors,
                    waypoints,
                    request.userAnswers.period,
                    periodIndex,
                    countryIndex,
                    country,
                    correctionReturnPeriod,
                    calculatedCorrectionAmount,
                    request.isIntermediary,
                    request.companyName
                  )
                ).toFuture,
              value =>

                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(VatPayableForCountryPage(periodIndex, countryIndex), value))
                } yield Redirect(VatPayableForCountryPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
            )
          }
        }
      }
  }
}
