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

import base.SpecBase
import connectors.ReturnStatusConnector
import models.core.{Match, MatchType}
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.*
import models.{PartialReturnPeriod, PeriodWithStatus, RegistrationWrapper, SubmissionStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import services.core.CoreRegistrationValidationService
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global

class PartialReturnPeriodServiceSpec extends SpecBase with BeforeAndAfterEach {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockReturnStatusConnector = mock[ReturnStatusConnector]
  private val mockPeriodService = mock[PeriodService]
  private val mockCoreRegValidationService = mock[CoreRegistrationValidationService]

  private val genericMatch = Match(
    MatchType.TransferringMSID,
    "333333333",
    None,
    "DE",
    None,
    None,
    None,
    None,
    None
  )

  override def beforeEach(): Unit = {
    Mockito.reset(mockReturnStatusConnector)
    Mockito.reset(mockPeriodService)
  }

  "PartialReturnPeriodService getPartialReturnPeriod should" - {

    "return a partial return period when it's the first return and transferring msid" in {

      val startDate = period.lastDay.minusDays(10)
      val commencementDate = startDate.plusDays(1)

      val registrationWrapperWithExclusions: RegistrationWrapper = {
        val updatedSchemeDetails = registrationWrapper.registration.schemeDetails.copy(
          commencementDate = commencementDate
        )

        val updatedRegistration = registrationWrapper.registration.copy(
          exclusions = Seq.empty,
          schemeDetails = updatedSchemeDetails
        )

        registrationWrapper.copy(registration = updatedRegistration)
      }

      when(mockPeriodService.getNextPeriod(any())).thenReturn(period)
      when(mockCoreRegValidationService.searchIossScheme(any(), any(), any(), any())(any())) thenReturn
        Some(genericMatch.copy(exclusionEffectiveDate = Some(startDate.toString))).toFuture
      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))).toFuture

      val service = new PartialReturnPeriodService(mockReturnStatusConnector, mockCoreRegValidationService)

      val result = service.getPartialReturnPeriod(registrationWrapperWithExclusions, period).futureValue

      val expectedPartialReturnPeriod = Some(PartialReturnPeriod(startDate, period.lastDay, period.year, period.month))

      result mustBe expectedPartialReturnPeriod
    }

    "return a partial return for a excluded trader's final return" in {

      val excludedEffectiveDate = period.lastDay.minusDays(10)
      val endDate = excludedEffectiveDate.minusDays(1)
      val commencementDate = period.firstDay.plusDays(4)

      val transferringMSIDReason = EtmpExclusion(
        TransferringMSID,
        endDate,
        LocalDate.now(stubClockAtArbitraryDate).minusDays(1),
        quarantine = false
      )

      val registrationWrapperWithExclusions: RegistrationWrapper = {
        val updatedSchemeDetails = registrationWrapper.registration.schemeDetails.copy(
          commencementDate = commencementDate
        )

        val updatedRegistration = registrationWrapper.registration.copy(
          exclusions = Seq(transferringMSIDReason),
          schemeDetails = updatedSchemeDetails
        )

        registrationWrapper.copy(registration = updatedRegistration)
      }

      when(mockPeriodService.getNextPeriod(any())).thenReturn(period)
      when(mockCoreRegValidationService.searchIossScheme(any(), any(), any(), any())(any())) thenReturn None.toFuture
      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))).toFuture

      val service = new PartialReturnPeriodService(mockReturnStatusConnector, mockCoreRegValidationService)

      val result = service.getPartialReturnPeriod(registrationWrapperWithExclusions, period).futureValue

      val expectedPartialReturnPeriod = Some(PartialReturnPeriod(period.firstDay, endDate.minusDays(1), period.year, period.month))

      result mustBe expectedPartialReturnPeriod
    }

    "return None when there is no exclusion" in {

      val commencementDate = period.firstDay.plusDays(4)

      val registrationWrapperWithoutExclusions: RegistrationWrapper = {
        val updatedSchemeDetails = registrationWrapper.registration.schemeDetails.copy(
          commencementDate = commencementDate
        )

        val updatedRegistration = registrationWrapper.registration.copy(
          exclusions = Seq.empty,
          schemeDetails = updatedSchemeDetails
        )

        registrationWrapper.copy(registration = updatedRegistration)
      }

      when(mockPeriodService.getNextPeriod(any())).thenReturn(period)
      when(mockCoreRegValidationService.searchIossScheme(any(), any(), any(), any())(any())) thenReturn Some(genericMatch).toFuture
      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))).toFuture

      val service = new PartialReturnPeriodService(mockReturnStatusConnector, mockCoreRegValidationService)

      val result = service.getPartialReturnPeriod(registrationWrapperWithoutExclusions, period).futureValue

      result mustBe None
    }

    "return None when exclusion reason is not TransferringMSID" in {

      val endDate = period.lastDay.minusDays(10)
      val commencementDate = period.firstDay.plusDays(4)

      val otherExclusionReason = EtmpExclusion(
        CeasedTrade,
        period.firstDay,
        endDate,
        quarantine = false
      )

      val registrationWrapperWithOtherExclusion: RegistrationWrapper = {
        val updatedSchemeDetails = registrationWrapper.registration.schemeDetails.copy(
          commencementDate = commencementDate
        )

        val updatedRegistration = registrationWrapper.registration.copy(
          exclusions = Seq(otherExclusionReason),
          schemeDetails = updatedSchemeDetails
        )

        registrationWrapper.copy(registration = updatedRegistration)
      }

      when(mockPeriodService.getNextPeriod(any())).thenReturn(period)
      when(mockCoreRegValidationService.searchIossScheme(any(), any(), any(), any())(any())) thenReturn None.toFuture
      when(mockReturnStatusConnector.listStatuses(any())(any())) thenReturn
        Right(Seq(PeriodWithStatus(period, SubmissionStatus.Due))).toFuture

      val service = new PartialReturnPeriodService(mockReturnStatusConnector, mockCoreRegValidationService)

      val result = service.getPartialReturnPeriod(registrationWrapperWithOtherExclusion, period).futureValue

      result mustBe None
    }

  }



}
