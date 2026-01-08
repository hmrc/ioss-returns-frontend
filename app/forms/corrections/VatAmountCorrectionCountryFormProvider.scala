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

package forms.corrections

import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class VatAmountCorrectionCountryFormProvider @Inject() extends Mappings {
  def apply(country: String, maximumCorrectionAmount: BigDecimal, isIntermediary: Boolean = false): Form[BigDecimal] = {
    val requiredKey = if(isIntermediary) "vatAmountCorrectionCountry.intermediary.error.required" else "vatAmountCorrectionCountry.error.required"
    val wholeNumberKey = if(isIntermediary) "vatAmountCorrectionCountry.intermediary.error.wholeNumber" else "vatAmountCorrectionCountry.error.wholeNumber"
    val nonNumericKey = if(isIntermediary) "vatAmountCorrectionCountry.intermediary.error.nonNumeric" else "vatAmountCorrectionCountry.error.nonNumeric"

    Form(
      "value" -> currency(
        requiredKey,
        wholeNumberKey,
        nonNumericKey,
        args = Seq(country))
        .verifying(nonZero("vatAmountCorrectionCountry.error.nonZero"))
        .verifying(minimumValueWithCurrency(-maximumCorrectionAmount, "vatAmountCorrectionCountry.error.negative"))
        .verifying(maximumValueWithCurrency(maximum = maxCurrencyAmount, "vatAmountCorrectionCountry.error.outOfRange.undeclared")))
  }
}