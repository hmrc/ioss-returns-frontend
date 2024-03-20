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

package forms.corrections

import forms.mappings.Mappings
import models.Country.euCountriesWithNI
import models.{Country, Index}
import play.api.data.Form

import javax.inject.Inject

class CorrectionCountryFormProvider @Inject() extends Mappings {

  def apply(index: Index, existingAnswers: Seq[Country]): Form[Country] = {
    val countries = euCountriesWithNI

    Form(
      "value" -> text("correctionCountry.error.required")
        .verifying("correctionCountry.error.required", value => countries.exists(_.code == value))
        .transform[Country](value => countries.find(_.code == value).get, _.code)
        .verifying(notADuplicate(index, existingAnswers, "correctionCountry.error.duplicate"))
    )
  }
}
