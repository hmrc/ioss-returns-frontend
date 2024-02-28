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

import javax.inject.Inject
import forms.mappings.Mappings
import play.api.data.Form
import viewmodels.previousReturns.PreviousRegistration

class ReturnRegistrationSelectionFormProvider @Inject() extends Mappings {

  def apply(previousRegistrations: Seq[PreviousRegistration]): Form[PreviousRegistration] =
    Form(
      "value" -> text("returnRegistrationSelection.error.required")
        .verifying("returnRegistrationSelection.error.required", value => previousRegistrations.exists(_.iossNumber == value))
        .transform[PreviousRegistration](value => previousRegistrations.find(_.iossNumber == value).get, _.iossNumber)
    )
}
