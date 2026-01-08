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

package utils

import controllers.actions.AuthenticatedControllerComponents
import models.VatRateFromCountry
import models.requests.DataRequest
import pages.{JourneyRecoveryPage, QuestionPage, Waypoints}
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContent, Result}
import play.api.mvc.Results.Redirect
import queries.{Derivable, Settable}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}
import viewmodels.govuk.checkbox._
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.Month


object ItemsHelper {

  def getDerivedItems(waypoints: Waypoints, derivable: Derivable[Seq[JsObject], Int])(block: Int => Future[Result])
                     (implicit request: DataRequest[AnyContent]): Future[Result] = {
    request.userAnswers.get(derivable).map {
      number =>
        block(number)
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
  }

  def determineRemoveAllItemsAndRedirect[A](
                                             waypoints: Waypoints,
                                             doRemoveItems: Boolean,
                                             cc: AuthenticatedControllerComponents,
                                             query: Settable[A],
                                             hasItems: QuestionPage[Boolean],
                                             deleteAllItemsPage: QuestionPage[Boolean]
                                           )(implicit ec: ExecutionContext, request: DataRequest[AnyContent]): Future[Result] = {
    val removeItems = if (doRemoveItems) {
      request.userAnswers.remove(query)
    } else {
      request.userAnswers.set(hasItems, true)
    }
    for {
      updatedAnswers <- Future.fromTry(removeItems)
      calculatedAnswers <- Future.fromTry(updatedAnswers.set(deleteAllItemsPage, doRemoveItems))
      _ <- cc.sessionRepository.set(calculatedAnswers)
    } yield Redirect(deleteAllItemsPage.navigate(waypoints, request.userAnswers, calculatedAnswers).route)
  }

  def checkboxItems(vatRates: Seq[VatRateFromCountry]): Seq[CheckboxItem] =
    vatRates.zipWithIndex.map {
      case (vatRate, index) =>
        CheckboxItemViewModel(
          content = Text(vatRate.rateForDisplay),
          fieldId = "value",
          index = index,
          value = vatRate.rate.toString
        )
    }

  def radioButtonItems(obligationYears: Seq[Int]): Seq[RadioItem] =
    obligationYears.map {
      case (years) =>
        RadioItem(
          content = Text(years.toString),
          value   = Some(years.toString),
          id      = Some(s"value_$years")
        )
    }

  def radioButtonMonthItems(obligationMonths: Seq[Month]): Seq[RadioItem] =
    obligationMonths.map {
      case (months) =>
        RadioItem(
          content = Text(months.toString.toLowerCase.capitalize),
          value   = Some(months.toString.toLowerCase.capitalize),
          id      = Some(s"value_$months")
        )
    }
}
