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

package forms

import forms.behaviours.CheckboxFieldBehaviours
import generators.Generators
import models.VatRateFromCountry
import org.scalacheck.Arbitrary.arbitrary
import play.api.data.FormError

class VatRatesFromCountryFormProviderSpec extends CheckboxFieldBehaviours with Generators {

  private val vatRateFromCountry1 = arbitrary[VatRateFromCountry].sample.value
  private val vatRateFromCountry2 = arbitrary[VatRateFromCountry].retryUntil(_ != vatRateFromCountry1).sample.value
  private val vatRateFromCountry3 = arbitrary[VatRateFromCountry].retryUntil(v => !List(vatRateFromCountry1, vatRateFromCountry2).contains(v)).sample.value
  private val vatRatesFromCountries = List(vatRateFromCountry1, vatRateFromCountry2)
  private val formProvider = new VatRatesFromCountryFormProvider()
  private val form = formProvider(vatRatesFromCountries)

  ".value" - {

    val fieldName = "value"
    val requiredKey = "vatRatesFromCountry.error.required"

    "must bind all valid values" in {

      val data =
        Map(
          s"$fieldName[0]" -> vatRatesFromCountries.head.rate.toString,
          s"$fieldName[1]" -> vatRatesFromCountries.tail.head.rate.toString
        )

      val result = form.bind(data)
      result.get mustEqual vatRatesFromCountries
      result.errors mustBe empty
    }

    "must fail to bind invalid values" in {

      val data = Map(s"$fieldName[0]" -> vatRateFromCountry3.rate.toString)
      form.bind(data).errors must contain(FormError(fieldName, "vatRatesFromCountry.error.invalid"))
    }

    "must fail to bind when the key is not present" in {

      val data = Map.empty[String, String]
      form.bind(data).errors must contain theSameElementsAs Seq(FormError(fieldName, requiredKey))
    }

    "must fail to bind when no answer is selected" in {

      val data = Map(s"$fieldName[0]" -> "")
      form.bind(data).errors must contain theSameElementsAs Seq(FormError(s"$fieldName[0]", requiredKey))
    }
  }
}
