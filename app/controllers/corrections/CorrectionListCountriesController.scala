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
import forms.corrections.CorrectionListCountriesFormProvider
import models.{Country, Index}
import pages.Waypoints
import pages.corrections.{CorrectionListCountriesPage, CorrectionReturnPeriodPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.DeriveNumberOfCorrections
import services.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ConvertPeriodKey
import utils.ItemsHelper.getDerivedItems
import viewmodels.checkAnswers.corrections.CorrectionListCountriesSummary
import views.html.corrections.CorrectionListCountriesView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionListCountriesController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: CorrectionListCountriesFormProvider,
                                         obligationsService: ObligationsService,
                                         view: CorrectionListCountriesView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with VatCorrectionBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val fulfilledObligations = obligationsService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>
        val periodKeys = obligations.map(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey))

        val correctionMonths = obligations.map(obligation => ConvertPeriodKey.monthNameFromEtmpPeriodKey(obligation.periodKey))

        val monthAndYear = s"${correctionMonths.mkString(", ")} ${periodKeys.mkString(", ")}"

        if (request.userAnswers.get(CorrectionReturnPeriodPage[String](periodIndex)).isDefined) {
          getNumberOfCorrectionsAsync(periodIndex) {
            (number, correctionPeriod) =>

              val canAddCountries = number < Country.euCountriesWithNI.size
              val list = CorrectionListCountriesSummary
                .addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))

              Future.successful(Ok(view(form, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries)))
          }
        } else {
          getDerivedItems(waypoints, DeriveNumberOfCorrections(periodIndex)) {
            number =>
              val canAddCountries = number < Country.euCountriesWithNI.size
              val list = CorrectionListCountriesSummary
                .addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))
              val correctionPeriod = monthAndYear

              Future.successful(Ok(view(form, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries)))

          }
        }
      }

  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period
      val fulfilledObligations = obligationsService.getFulfilledObligations(request.iossNumber)

      fulfilledObligations.flatMap { obligations =>
        val periodKeys = obligations.map(obligation => ConvertPeriodKey.yearFromEtmpPeriodKey(obligation.periodKey))
        val correctionMonths = obligations.map(obligation => ConvertPeriodKey.monthNameFromEtmpPeriodKey(obligation.periodKey))
        val monthAndYear = s"${correctionMonths.mkString(", ")} ${periodKeys.mkString(", ")}"

        if (request.userAnswers.get(CorrectionReturnPeriodPage[String](periodIndex)).isDefined) {
          getNumberOfCorrectionsAsync(periodIndex) {
            (number, correctionPeriod) =>
              val canAddCountries = number < Country.euCountriesWithNI.size
              val list = CorrectionListCountriesSummary
                .addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))

              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries))),

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionListCountriesPage(periodIndex), value))
                    _ <- cc.sessionRepository.set(updatedAnswers)
                  } yield Redirect(CorrectionListCountriesPage(periodIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route))
          }
        } else {
          getDerivedItems(waypoints, DeriveNumberOfCorrections(periodIndex)) {
            number =>

              val canAddCountries = number < Country.euCountriesWithNI.size
              val list = CorrectionListCountriesSummary.addToListRows(request.userAnswers, waypoints, periodIndex, CorrectionListCountriesPage(periodIndex))
              val correctionPeriod = monthAndYear

              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(formWithErrors, waypoints, list, period, correctionPeriod, periodIndex, canAddCountries))),

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(CorrectionListCountriesPage(periodIndex), value))
                    _ <- cc.sessionRepository.set(updatedAnswers)
                  } yield Redirect(CorrectionListCountriesPage(periodIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route))
          }
        }
      }
  }


}
