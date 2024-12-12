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

package models.requests.corrections

import base.SpecBase
import models.Country
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}

class CorrectionRequestSpec extends SpecBase {

  "CorrectionRequest" - {

    "create a CorrectionRequest with the correct values" in {

      val corrections = List(
        PeriodWithCorrections(
          correctionReturnPeriod = period,
          correctionsToCountry = Some(List(CorrectionToCountry(
            Country("ES", "Spain"),
            Some(BigDecimal(500))
          )))
        )
      )

      val correctionRequest = CorrectionRequest(vrn, period, corrections)

      correctionRequest.vrn mustBe vrn
      correctionRequest.period mustBe period
      correctionRequest.corrections mustBe corrections
    }

    "handle None for correctionsToCountry correctly" in {

      val corrections = List(
        PeriodWithCorrections(
          correctionReturnPeriod = period,
          correctionsToCountry = None
        )
      )

      val correctionRequest = CorrectionRequest(vrn, period, corrections)

      correctionRequest.corrections.head.correctionsToCountry mustBe None
    }
  }

}
