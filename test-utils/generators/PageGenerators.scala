/*
 * Copyright 2025 HM Revenue & Customs
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

package generators

import models.Index
import org.scalacheck.Arbitrary

import pages.corrections._

trait PageGenerators {

  implicit lazy val arbitraryUndeclaredCountryCorrectionPage: Arbitrary[UndeclaredCountryCorrectionPage] =
    Arbitrary(UndeclaredCountryCorrectionPage(Index(0), Index(0)))

  implicit lazy val arbitraryRemovePeriodCorrectionPage: Arbitrary[RemovePeriodCorrectionPage.type] =
    Arbitrary(RemovePeriodCorrectionPage)

  implicit lazy val arbitraryRemoveCountryCorrectionPage: Arbitrary[RemoveCountryCorrectionPage.type] =
    Arbitrary(RemoveCountryCorrectionPage)

  implicit lazy val arbitraryCountryVatCorrectionPage: Arbitrary[VatAmountCorrectionCountryPage] =
    Arbitrary(VatAmountCorrectionCountryPage(Index(0), Index(0)))

  implicit lazy val arbitraryCorrectionReturnPeriodPage: Arbitrary[CorrectionReturnPeriodPage.type] =
    Arbitrary(CorrectionReturnPeriodPage)

  implicit lazy val arbitraryCorrectionCountryPage: Arbitrary[CorrectionCountryPage] =
    Arbitrary(CorrectionCountryPage(Index(0), Index(0)))

  implicit lazy val arbitraryCorrectPreviousReturnPage: Arbitrary[CorrectPreviousReturnPage.type] =
    Arbitrary(CorrectPreviousReturnPage)
}
