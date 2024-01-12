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

import controllers.corrections.VatPeriodCorrectionsOpSyntax.{CorrectionsWithCountriesInAnswersWhereNoSubmissionMadeOps, PeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPageOps}
import controllers.{routes => baseRoutes}
import models.etmp.EtmpObligationsFulfilmentStatus
import models.requests.DataRequest
import models.{Index, Period}
import pages.Waypoints
import play.api.mvc.Results.Redirect
import play.api.mvc.{AnyContent, Result}
import queries.DeriveCompletedCorrectionPeriods
import services.VatPeriodCorrectionsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


trait VatPeriodCorrections {
  def findCorrectionsWithCountriesInAnswersWhereNoSubmissionMade(vatPeriodCorrectionsService: VatPeriodCorrectionsService)(block: => Result) = {
    CorrectionsWithCountriesInAnswersWhereNoSubmissionMadeOps((vatPeriodCorrectionsService, block))
  }

  def findPeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPage(waypoints: Waypoints,
                                                                                     period: Period,
                                                                                     incompletePromptShown: Boolean,
                                                                                     request: DataRequest[AnyContent],
                                                                                     incompletePeriodsWithCountryButNoVatCorrection: List[Period]
                                                                                    ): PeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPageOps =
    PeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPageOps(
      (waypoints, period, incompletePromptShown, request, incompletePeriodsWithCountryButNoVatCorrection)
    )
}

object VatPeriodCorrectionsOpSyntax {

  case class CorrectionsWithCountriesInAnswersWhereNoSubmissionMadeOps(value: (VatPeriodCorrectionsService, Result)) {

    def orWhenEmpty(whenEmptyBlock:
                    => Result
                   )(
                     implicit request: DataRequest[AnyContent],
                     hc: HeaderCarrier,
                     executionContext: ExecutionContext
                   ):
    Future[Result] = {
      val (vatPeriodCorrectionsService, block) = value

      vatPeriodCorrectionsService.listStatuses(request.registrationWrapper.registration.schemeDetails.commencementDate)
        .map {
          returnStatuses =>
            val allPeriodsOfCompletedStatuses = returnStatuses.filter(obligationDetails =>
              obligationDetails.status.equals(EtmpObligationsFulfilmentStatus.Fulfilled))
              .map(_.periodKey)
            if (allPeriodsOfCompletedStatuses.isEmpty) {
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            } else {
              val correctionPeriodsWhereCountryCorrectionsEntered: List[Period] = request.userAnswers.get(DeriveCompletedCorrectionPeriods).getOrElse(List())

              val uncompletedCorrectionPeriods: List[Period] = allPeriodsOfCompletedStatuses
                .map(Period.fromKey(_))
                .diff(correctionPeriodsWhereCountryCorrectionsEntered).distinct.toList

              if (uncompletedCorrectionPeriods.nonEmpty) {
                block
              } else {
                whenEmptyBlock
              }
            }
        }
    }
  }

  case class PeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPageOps(
                                                                                            value: (
                                                                                              Waypoints,
                                                                                                Period,
                                                                                                Boolean,
                                                                                                DataRequest[AnyContent],
                                                                                                List[Period]
                                                                                              )
                                                                                          ) {
    def orWhenEmpty(blockForIncompleteEmpty: => Result): Result = {
      val (_, _, _, _, incompletePeriods) = value
      if (incompletePeriods.isEmpty) {
        blockForIncompleteEmpty
      } else {
        findPeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPage()
      }
    }

    private def findPeriodOfCountryCorrectionsWithNoVatCorrectionInAnswersAndShowCountriesPage(): Result = {
      val (waypoints, period, incompletePromptShown, request, incompletePeriodsWithCountryButNoVatCorrection) = value
      if (incompletePromptShown) {
        val correctionPeriodsWhereCountryCorrectionsEntered = request.userAnswers.get(DeriveCompletedCorrectionPeriods)
          .getOrElse(List.empty).zipWithIndex

        getPeriodIndexOfNoVatCorrection(
          correctionPeriodsWhereCountryCorrectionsEntered,
          incompletePeriodsWithCountryButNoVatCorrection
        ).map(index =>
          Redirect(controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, Index(index)))
        )
          .getOrElse(Redirect(baseRoutes.JourneyRecoveryController.onPageLoad()))
      } else {
        Redirect(routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, period))
      }
    }

    private def getPeriodIndexOfNoVatCorrection(
                                                 correctionPeriodsWhereCountryCorrectionsEntered: List[(Period, Int)],
                                                 incompletePeriodsWithCountryButNoVatCorrection: List[Period]): Option[Int] = {
      correctionPeriodsWhereCountryCorrectionsEntered.find(correctionPeriod => {
        val (period, _) = correctionPeriod
        period == incompletePeriodsWithCountryButNoVatCorrection.head
      })
        .map(correctionPeriod => {
          val (_, periodIndex) = correctionPeriod
          periodIndex
        })
    }
  }

}