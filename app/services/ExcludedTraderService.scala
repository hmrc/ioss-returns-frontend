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

import models.Period
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{CeasedTrade, FailsToComply, NoLongerMeetsConditions, NoLongerSupplies, TransferringMSID, VoluntarilyLeaves}

class ExcludedTraderService() {

  def isExcludedPeriod(exclusion: EtmpExclusion, period: Period): Boolean = {
    val isExcludedFirstDay: Boolean =
      exclusion.exclusionReason == TransferringMSID && period.firstDay.isAfter(exclusion.effectiveDate)

    val isExcludedLastDay: Boolean = Seq(
      NoLongerSupplies,
      VoluntarilyLeaves,
      CeasedTrade,
      NoLongerMeetsConditions,
      FailsToComply
    ).contains(exclusion.exclusionReason) && period.lastDay.isAfter(exclusion.effectiveDate)

    isExcludedFirstDay || isExcludedLastDay
  }

}
