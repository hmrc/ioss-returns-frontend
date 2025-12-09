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

package forms

import forms.mappings.Mappings
import models.VatRateFromCountry
import play.api.data.Form
import services.VatRateService

import javax.inject.Inject

class SalesToCountryFormProvider @Inject()(vatRateService: VatRateService) extends Mappings {

  def apply(vatRate: VatRateFromCountry, isIntermediary: Boolean = false): Form[BigDecimal] = {
    val requiredKey = if(isIntermediary) "salesToCountry.intermediary.error.required" else "salesToCountry.error.required"
    val wholeNumberKey = if(isIntermediary) "salesToCountry.intermediary.error.wholeNumber" else "salesToCountry.error.wholeNumber"
    val nonNumericKey = if(isIntermediary) "salesToCountry.intermediary.error.nonNumeric" else "salesToCountry.error.nonNumeric"

    Form(
      "value" -> currency(
        requiredKey,
        wholeNumberKey,
        nonNumericKey,
        args = Seq(vatRate.rateForDisplay))
        .verifying(inRange[BigDecimal](0, maxCurrencyAmount, "salesToCountry.error.outOfRange"))
        .verifying("salesToCountry.error.calculatedVatRateOutOfRange", value => {
          vatRateService.standardVatOnSales(value, vatRate) > BigDecimal(0)
        })
    )
  }
}