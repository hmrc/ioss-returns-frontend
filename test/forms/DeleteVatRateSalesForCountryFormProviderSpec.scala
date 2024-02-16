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

import forms.behaviours.BooleanFieldBehaviours
import models.Country
import play.api.data.FormError

class DeleteVatRateSalesForCountryFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey = "deleteVatRateSalesForCountry.error.required"
  private val invalidKey = "error.boolean"
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val vatRate: String = arbitraryVatRateFromCountry.arbitrary.sample.value.rateForDisplay

  private val form = new DeleteVatRateSalesForCountryFormProvider()(vatRate, country)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, Seq(vatRate, country.name))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(vatRate, country.name))
    )
  }
}
