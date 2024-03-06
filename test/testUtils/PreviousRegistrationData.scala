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

package testUtils

import base.SpecBase
import models.Period
import viewmodels.previousReturns.{PreviousRegistration, SelectedPreviousRegistration}

import java.time.YearMonth

object PreviousRegistrationData extends SpecBase {

  val previousRegistrationIM900987654321: PreviousRegistration = PreviousRegistration(
    "IM900987654321",
    Period(YearMonth.of(2020, 1)),
    Period(YearMonth.of(2021, 2))
  )

  val previousRegistrationIM900987654322: PreviousRegistration = PreviousRegistration(
    "IM900987654322",
    Period(YearMonth.of(2021, 3)),
    Period(YearMonth.of(2021, 10))
  )

  val selectedPreviousRegistration: SelectedPreviousRegistration = SelectedPreviousRegistration(userAnswersId, previousRegistrationIM900987654322)

  val previousRegistrations: List[PreviousRegistration] = List(previousRegistrationIM900987654321, previousRegistrationIM900987654322)
}
