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

package services

import cats.data.Validated.Invalid
import controllers._
import controllers.actions.AuthenticatedControllerComponents
import controllers.corrections.{routes => correctionsRoutes}
import logging.Logging
import models.requests.DataRequest
import models.{DataMissingError, Index, Period, ValidationError}
import pages.corrections.CorrectPreviousReturnPage
import pages.{VatRatesFromCountryPage, Waypoints}
import play.api.i18n.I18nSupport
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents}
import queries.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, AllSalesQuery, CorrectionToCountryQuery, SalesAtVatRateQuery, VatOnSalesFromQuery}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RedirectService @Inject()(
                               cc: AuthenticatedControllerComponents,
                               correctionService: CorrectionService,
                               vatReturnService: VatReturnService
                               )(implicit ec:ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def validate(period: Period)(implicit request: DataRequest[AnyContent]): List[ValidationError] = {

    val commencementDate = request.registrationWrapper.registration.schemeDetails.commencementDate

    val validateVatReturnRequest = vatReturnService.fromUserAnswers(request.userAnswers, request.vrn, period)

    val validateCorrectionRequest = request.userAnswers.get(CorrectPreviousReturnPage(0)).map(_ =>
      correctionService.fromUserAnswers(request.userAnswers, request.vrn, period, commencementDate))

    (validateVatReturnRequest, validateCorrectionRequest) match {
      case (Invalid(vatReturnErrors), Some(Invalid(correctionErrors))) =>
        (vatReturnErrors ++ correctionErrors).toChain.toList
      case (Invalid(errors), _) =>
        errors.toChain.toList
      case (_, Some(Invalid(errors))) =>
        errors.toChain.toList
      case _ => List.empty[ValidationError]
    }
  }

  def getRedirect(waypoints: Waypoints, errors: List[ValidationError]): List[Call] = {
    errors.flatMap {
          //sales
      case DataMissingError(AllSalesQuery) =>
        logger.error(s"Data missing - no data provided for sales")
        Some(routes.SoldToCountryController.onPageLoad(waypoints, Index(0)))

      case DataMissingError(VatRatesFromCountryPage(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat rates with index ${vatRateIndex.position}")
        Some(routes.VatRatesFromCountryController.onPageLoad(waypoints, countryIndex))

      case DataMissingError(SalesAtVatRateQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat rates with index ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.SalesToCountryController.onPageLoad(waypoints, countryIndex, vatRateIndex))

      case DataMissingError(VatOnSalesFromQuery(countryIndex, vatRateIndex)) =>
        logger.error(s"Data missing - vat charged on sales at vat rate ${vatRateIndex.position} for country ${countryIndex.position}")
        Some(routes.VatOnSalesController.onPageLoad(waypoints, countryIndex, vatRateIndex))

        //corrections
      case DataMissingError(AllCorrectionPeriodsQuery) =>
        logger.error(s"Data missing - no data provided for corrections")
        Some(correctionsRoutes.CorrectionReturnPeriodController.onPageLoad(waypoints, Index(0)))
      case DataMissingError(AllCorrectionCountriesQuery(periodIndex)) =>
        logger.error(s"Data missing - no countries found for corrections to period ${periodIndex.position}")
        Some(correctionsRoutes.CorrectionCountryController.onPageLoad(waypoints, periodIndex, Index(0)))
      case DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)) =>
        logger.error(s"Data missing - correction to country ${countryIndex.position} in period ${periodIndex.position}")
        Some(correctionsRoutes.VatAmountCorrectionCountryController.onPageLoad(waypoints, periodIndex, Index(0)))

      case DataMissingError(_) =>
        logger.error(s"Unhandled DataMissingError")
        None
      case _ =>
        logger.error(s"Unhandled ValidationError")
        None
    }
  }
}
