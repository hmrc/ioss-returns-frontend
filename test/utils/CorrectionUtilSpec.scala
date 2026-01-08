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

import base.SpecBase
import testUtils.EtmpVatReturnCorrectionsData._

class CorrectionUtilSpec extends SpecBase {


  ".calculateNegativeAndZeroCorrections" - {

    "must return a calculated negative balance for a single country with negative corrections" in {

      val result = CorrectionUtil.calculateNegativeAndZeroCorrections(etmpVatReturnCorrectionSingleCountryScenario)

      result mustBe Map("DE" -> BigDecimal(-2900))
    }

    "must return a calculated negative balance for multiple counties with negative corrections" in {

      val result = CorrectionUtil.calculateNegativeAndZeroCorrections(etmpVatReturnCorrectionMultipleCountryScenario)

      result mustBe Map("LT" -> BigDecimal(-1050), "IT" -> BigDecimal(-2350))
    }

    "must return a nil balance for multiple counties with negative corrections" in {

      val result = CorrectionUtil.calculateNegativeAndZeroCorrections(etmpVatReturnCorrectionMultipleCountryNilScenario)

      result mustBe Map("EE" -> BigDecimal(0), "PL" -> BigDecimal(0))
    }

    "must return a calculated negative balance for multiple counties with both negative and positive corrections" in {

      val result = CorrectionUtil.calculateNegativeAndZeroCorrections(etmpVatReturnCorrectionMultipleCountryMixPosAndNegCorrectionsScenario)

      result mustBe Map("AT" -> BigDecimal(-350), "HR" -> BigDecimal(-50))
    }
  }
}
