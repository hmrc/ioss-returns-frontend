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

import connectors.{RegistrationConnector, VatReturnConnector}
import controllers.actions._
import forms.corrections.CorrectionCountryFormProvider
import models.{Index, Period}
import pages.Waypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AllCorrectionCountriesQuery, CorrectionPeriodQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionCountryView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CorrectionCountryController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             cc: AuthenticatedControllerComponents,
                                             formProvider: CorrectionCountryFormProvider,
                                             vatReturnConnector: VatReturnConnector,
                                             view: CorrectionCountryView
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with CorrectionBaseController {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, index: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      getCountry(waypoints, periodIndex, index) { country =>

        val period = request.userAnswers.period

        val periodFrom = request.userAnswers.get(CorrectionPeriodQuery(periodIndex)).map(_.correctionReturnPeriod).getOrElse()

        val x = Future.sequence(getAllPeriods(periodFrom, period).sortBy(_.firstDay).map(vatReturnConnector.get))

//         Get original vat return and filter by country and get vat amount
          val originalVatReturnAmount = for {
            etmpVatReturnResult <- vatReturnConnector.get(period)
          } yield etmpVatReturnResult.map { vatReturn =>
            vatReturn.goodsSupplied.filter(_.msOfConsumption == country.code)

          }

        val form = formProvider(
          index,
          request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
            .getOrElse(Seq.empty)
            .map(_.correctionCountry)
        )

        val preparedForm = request.userAnswers.get(CorrectionCountryPage(periodIndex, index)) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex)) match {
          case Some(correctionPeriod) => Ok(view(preparedForm, waypoints, period, periodIndex, correctionPeriod, index))
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url)
        }
      }
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, index: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      val form = formProvider(
        index,
        request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
          .getOrElse(Seq.empty)
          .map(_.correctionCountry)
      )

      form.bindFromRequest().fold(
        formWithErrors =>
          request.userAnswers.get(CorrectionReturnPeriodPage(periodIndex)) match {
            case Some(correctionPeriod) => Future.successful(BadRequest (view (formWithErrors, waypoints, period, periodIndex, correctionPeriod, index)))
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad().url))
          },
        value =>
          for {
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(CorrectionCountryPage(periodIndex, index), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(CorrectionCountryPage(periodIndex, index).navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }

  def getAllPeriods(periodFrom: Period, periodTo: Period): Seq[Period] = {

    def getAllPeriodsInRange(currentPeriods: Seq[Period], periodFrom: Period, periodTo: Period): Seq[Period] = {
      (periodFrom, periodTo) match {
        case (pf, pt) if pf.isBefore(pt) =>
          val updatedPeriod = currentPeriods :+ pf
          getAllPeriodsInRange(updatedPeriod, pf.getNext, pt)
        case _ => currentPeriods
      }
    }

    getAllPeriodsInRange(Seq.empty, periodFrom, periodTo)
  }
}
