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


import connectors.ReturnStatusConnector
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.TransferringMSID
import models.{PartialReturnPeriod, Period, PeriodWithStatus, RegistrationWrapper, SubmissionStatus}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartialReturnPeriodService @Inject()(
                                            returnStatusConnector: ReturnStatusConnector,
                                            periodService: PeriodService,
                                          )(implicit ec: ExecutionContext) {

  def getPartialReturnPeriod(
                              registrationWrapper: RegistrationWrapper,
                              period: Period
                            )(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {

    val maybeExclusion: Option[EtmpExclusion] = registrationWrapper.registration.exclusions.lastOption

    maybeExclusion match {
      case None =>
        getMaybeFirstPartialReturnPeriod(registrationWrapper, maybeExclusion)
      case Some(excludedTrader) =>
        excludedTrader.exclusionReason match {
          case TransferringMSID =>
            if (isFinalReturn(maybeExclusion, period)) {
              Future.successful(Some(PartialReturnPeriod(
                period.firstDay,
                excludedTrader.effectiveDate,
                period.year,
                period.month
              )))
            } else {
              getMaybeFirstPartialReturnPeriod(registrationWrapper, maybeExclusion)
            }
          case _ => Future.successful(None)
        }
    }
  }



  def getMaybeFirstPartialReturnPeriod(
                                                registrationWrapper: RegistrationWrapper,
                                                maybeExclusion: Option[EtmpExclusion]
                                              )(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {

    val commencementDateString = registrationWrapper.registration.schemeDetails.commencementDate
    val commencementDate = LocalDate.parse(commencementDateString)

    val effectiveDate = maybeExclusion.map { exclusions =>
      exclusions.effectiveDate
    }

    returnStatusConnector.listStatuses(commencementDate).map {
      case Right(returnsPeriod) if isFirstPeriod(returnsPeriod, commencementDate) =>
        val firstReturnPeriod = returnsPeriod.head.period
        effectiveDate.flatMap {
          case transferringMsidEffectiveFromDate if isWithinPeriod(firstReturnPeriod, transferringMsidEffectiveFromDate) =>
            Some(PartialReturnPeriod(
              transferringMsidEffectiveFromDate,
              firstReturnPeriod.lastDay,
              firstReturnPeriod.year,
              firstReturnPeriod.month
            ))
          case _ =>
            None
        }
      case _ =>
        None
    }
  }

  private def isFirstPeriod(periods: Seq[PeriodWithStatus], checkDate: LocalDate): Boolean = {
    val firstUnsubmittedPeriod = periods.filter(period =>
      Seq(SubmissionStatus.Next, SubmissionStatus.Due, SubmissionStatus.Overdue).contains(period.status)
    ).head
    isWithinPeriod(firstUnsubmittedPeriod.period, checkDate)
  }

  private def isWithinPeriod(period: Period, checkDate: LocalDate): Boolean =
    !checkDate.isBefore(period.firstDay) &&
      !checkDate.isAfter(period.lastDay)

  private def isFinalReturn(maybeExclusion: Option[EtmpExclusion], period: Period): Boolean = {

    val nextPeriodString = periodService.getNextPeriod(period).displayYearMonth
    val nextPeriod: LocalDate = LocalDate.parse(nextPeriodString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    maybeExclusion.fold(false) { exclusions =>
      nextPeriod.isAfter(exclusions.effectiveDate)
    }
  }


}
