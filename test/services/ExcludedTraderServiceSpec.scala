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

package services

import base.SpecBase
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason._

import java.time.LocalDate

class ExcludedTraderServiceSpec extends SpecBase {

  private val excludedTraderService = new ExcludedTraderService()

  "isExcludedPeriod" - {
    "return true when TransferringMSID and period's first day is after effective date" in {
      val effectiveDate = period.firstDay.minusDays(1)
      val exclusion = EtmpExclusion(TransferringMSID, effectiveDate = effectiveDate, LocalDate.now, false)
      excludedTraderService.isExcludedPeriod(exclusion, period) mustBe true
    }

    "return false when TransferringMSID and period's first day is before effective date" in {
      val effectiveDate = period.firstDay.plusDays(1)
      val exclusion = EtmpExclusion(TransferringMSID, effectiveDate = effectiveDate, LocalDate.now, false)
      excludedTraderService.isExcludedPeriod(exclusion, period) mustBe false
    }

    "return false when TransferringMSID and period's first day is equal to effective date" in {
      val effectiveDate = period.firstDay
      val exclusion = EtmpExclusion(TransferringMSID, effectiveDate = effectiveDate, LocalDate.now, false)
      excludedTraderService.isExcludedPeriod(exclusion, period) mustBe false
    }

    "return true when NoLongerSupplies, VoluntarilyLeaves, CeasedTrade, NoLongerMeetsConditions, FailsToComply and " +
      "period's last day is after effective date" in {
      Seq(
        NoLongerSupplies,
        VoluntarilyLeaves,
        CeasedTrade,
        NoLongerMeetsConditions,
        FailsToComply
      ).map { exclusionReason =>
        val effectiveDate = period.lastDay.minusDays(1)
        val exclusion = EtmpExclusion(exclusionReason, effectiveDate = effectiveDate, LocalDate.now, false)
        excludedTraderService.isExcludedPeriod(exclusion, period) mustBe true
      }
    }

    "return false when NoLongerSupplies, VoluntarilyLeaves, CeasedTrade, NoLongerMeetsConditions, FailsToComply and " +
      "period's last day is before effective date" in {
      Seq(
        NoLongerSupplies,
        VoluntarilyLeaves,
        CeasedTrade,
        NoLongerMeetsConditions,
        FailsToComply
      ).map { exclusionReason =>
        val effectiveDate = period.lastDay.plusDays(1)
        val exclusion = EtmpExclusion(exclusionReason, effectiveDate = effectiveDate, LocalDate.now, false)
        excludedTraderService.isExcludedPeriod(exclusion, period) mustBe false
      }
    }

    "return false when NoLongerSupplies, VoluntarilyLeaves, CeasedTrade, NoLongerMeetsConditions, FailsToComply and " +
      "period's last day is equal to effective date" in {
      Seq(
        NoLongerSupplies,
        VoluntarilyLeaves,
        CeasedTrade,
        NoLongerMeetsConditions,
        FailsToComply
      ).map { exclusionReason =>
        val effectiveDate = period.lastDay
        val exclusion = EtmpExclusion(exclusionReason, effectiveDate = effectiveDate, LocalDate.now, false)
        excludedTraderService.isExcludedPeriod(exclusion, period) mustBe false
      }
    }
  }
}
