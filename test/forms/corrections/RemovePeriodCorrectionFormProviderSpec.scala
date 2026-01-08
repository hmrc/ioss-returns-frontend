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

import forms.behaviours.BooleanFieldBehaviours
import models.Period
import play.api.data.FormError

class RemovePeriodCorrectionFormProviderSpec extends BooleanFieldBehaviours {

  private val requiredKey: String = "removePeriodCorrection.error.required"
  private val invalidKey: String = "error.boolean"
  private val period: Period = arbitraryPeriod.arbitrary.sample.value

  val form = new RemovePeriodCorrectionFormProvider()(period)

  ".value" - {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey, Seq(period.displayText))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey, Seq(period.displayText))
    )
  }
}
