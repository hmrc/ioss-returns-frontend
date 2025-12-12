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

package forms

import forms.behaviours.OptionFieldBehaviours
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import play.api.data.FormError
import testUtils.PreviousRegistrationData.previousRegistrations

class ReturnRegistrationSelectionFormProviderSpec extends OptionFieldBehaviours {

  val form = new ReturnRegistrationSelectionFormProvider()(previousRegistrations)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "returnRegistrationSelection.error.required"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf(previousRegistrations).map(_.iossNumber)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind any values other than valid ioss number" in {

      val invalidAnswers = arbitrary[String] suchThat (x => !previousRegistrations.exists(_.iossNumber == x))

      forAll(invalidAnswers) {
        answer =>
          val result = form.bind(Map("value" -> answer)).apply(fieldName)
          result.errors must contain only FormError(fieldName, requiredKey)
      }
    }
  }
}
