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

import forms.CorrectionReturnYearFormProvider
import forms.behaviours.OptionFieldBehaviours
import models.{Index, Period}
import play.api.data.FormError

import java.time.Month

class CorrectionReturnYearFormProviderSpec extends OptionFieldBehaviours {

  val testPeriods: Seq[Period] = Seq(Period(2021, Month.OCTOBER))
  val index: Index = Index(0)

  val form = new CorrectionReturnYearFormProvider()(index, Seq.empty)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "correctionReturnYear.error.required"

    behave like optionsField[Period](
      form,
      fieldName,
      validValues = testPeriods,
      invalidError = FormError(fieldName, "error.invalidPeriod")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
