/*
 * Copyright 2023 HM Revenue & Customs
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

import config.Constants.minCurrencyAmount
import forms.mappings.Mappings
import play.api.data.Form

import javax.inject.Inject

class VatAmountCorrectionCountryFormProvider @Inject() extends Mappings {
  def apply(country: String): Form[BigDecimal] =
    Form(
      "value" -> currency(
        "vatAmountCorrectionCountry.error.required",
        "vatAmountCorrectionCountry.error.wholeNumber",
        "vatAmountCorrectionCountry.error.nonNumeric",
        args = Seq(country))
        .verifying(inRange[BigDecimal](
          minCurrencyAmount, maxCurrencyAmount,
         "vatAmountCorrectionCountry.error.outOfRange.undeclared"))
        .verifying(nonZero("vatAmountCorrectionCountry.error.nonZero")))
}
