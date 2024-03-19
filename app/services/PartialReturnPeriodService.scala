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
import models.core.MatchType
import models.etmp.{EtmpExclusion, EtmpExclusionReason}
import models.etmp.EtmpExclusionReason.TransferringMSID
import models.etmp.SchemeType.{IOSSWithIntermediary, IOSSWithoutIntermediary}
import models.{PartialReturnPeriod, Period, PeriodWithStatus, RegistrationWrapper, StandardPeriod, SubmissionStatus}
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartialReturnPeriodService @Inject()(
                                            returnStatusConnector: ReturnStatusConnector,
                                            coreValidationService: CoreRegistrationValidationService,
                                            periodService: PeriodService,
                                          )(implicit ec: ExecutionContext) {

  def getPartialReturnPeriod(
                              registrationWrapper: RegistrationWrapper,
                              period: Period
                            )(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {

    val maybeExclusion: Option[EtmpExclusion] = registrationWrapper.registration.exclusions.headOption

    maybeExclusion match {
      case None =>
        getMaybeFirstPartialReturnPeriod(registrationWrapper)
      case Some(excludedTrader) =>
        excludedTrader.exclusionReason match {
          case TransferringMSID =>
            if (isFinalReturn(maybeExclusion, period)) {
              Some(PartialReturnPeriod(
                period.firstDay,
                excludedTrader.effectiveDate,
                period.year,
                period.month
              )).toFuture
            } else {
              None.toFuture
            }
          case _ => None.toFuture
        }
    }
  }

  private def getMaybeFirstPartialReturnPeriod(
                                        registrationWrapper: RegistrationWrapper
                                      )(implicit hc: HeaderCarrier): Future[Option[PartialReturnPeriod]] = {

    val commencementDateString = registrationWrapper.registration.schemeDetails.commencementDate
    val commencementDate = LocalDate.parse(commencementDateString.toString)
    val filteredDetails = getFilteredDetails(registrationWrapper)

    Future.sequence(
      filteredDetails.map { previousIossReg =>
        coreValidationService.searchIossScheme(
          searchNumber = previousIossReg.registrationNumber,
          previousScheme = previousIossReg.schemeType,
          intermediaryNumber = previousIossReg.intermediaryNumber,
          countryCode = previousIossReg.issuedBy
        ).flatMap {
          case Some(coreRegistrationMatch) if coreRegistrationMatch.matchType == MatchType.TransferringMSID =>
            coreRegistrationMatch.exclusionEffectiveDate match {
              case Some(transferringMsidEffectiveFromDate) =>
                val transferringMsidEffectiveLocalDate = LocalDate.parse(transferringMsidEffectiveFromDate)
                returnStatusConnector.listStatuses(commencementDate).map {
                  case Right(returnsPeriod) if isFirstPeriod(returnsPeriod, commencementDate) =>
                    val firstReturnPeriod = returnsPeriod.head.period
                    if (isWithinPeriod(firstReturnPeriod, commencementDate)) {
                      Some(PartialReturnPeriod(
                        transferringMsidEffectiveLocalDate,
                        firstReturnPeriod.lastDay,
                        firstReturnPeriod.year,
                        firstReturnPeriod.month
                      ))
                    } else {
                      None
                    }
                  case _ =>
                    None
                }
              case _ => None.toFuture
            }
          case _ => None.toFuture
        }
      }
    ).map(_.flatten.maxByOption(_.paymentDeadline))
  }


  private def isFirstPeriod(periods: Seq[PeriodWithStatus], checkDate: LocalDate): Boolean = {
    val firstUnsubmittedPeriod = periods.filter { period =>
      Seq(SubmissionStatus.Next, SubmissionStatus.Due, SubmissionStatus.Overdue).contains(period.status)
    }.head

    isWithinPeriod(firstUnsubmittedPeriod.period, checkDate)
  }

  private def isWithinPeriod(period: StandardPeriod, checkDate: LocalDate): Boolean =
    !checkDate.isBefore(period.firstDay) &&
      !checkDate.isAfter(period.lastDay)

  def isFinalReturn(maybeExclusion: Option[EtmpExclusion], period: Period): Boolean = {
    maybeExclusion match {
      case Some(exclusion) if exclusion.exclusionReason == EtmpExclusionReason.TransferringMSID =>
        isWithinPeriod(StandardPeriod.fromPeriod(period), exclusion.effectiveDate)
      case Some(exclusion) if exclusion.exclusionReason != EtmpExclusionReason.Reversal =>
        val nextPeriod = period.getNext.lastDay
        nextPeriod.isAfter(exclusion.effectiveDate)
      case _ => false
    }
  }

  private def getFilteredDetails(registrationWrapper: RegistrationWrapper) = {
    registrationWrapper.registration.schemeDetails.previousEURegistrationDetails match {
      case previousRegistrations if previousRegistrations.nonEmpty =>
        previousRegistrations.filter(
          details => details.schemeType == IOSSWithoutIntermediary || details.schemeType == IOSSWithIntermediary
        )
      case _ =>
        Seq.empty
    }
  }

}
