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

import config.Constants.maxCurrencyAmount
import forms.behaviours.DecimalFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import utils.CurrencyFormatter

import scala.math.BigDecimal.RoundingMode

class VatAmountCorrectionCountryFormProviderSpec extends DecimalFieldBehaviours {

  private val country = "Country"

  val minimum = 0
  val maximum: BigDecimal = maxCurrencyAmount
  val maximumCorrectionAmount: BigDecimal = 0

  val form = new VatAmountCorrectionCountryFormProvider()(country, maximumCorrectionAmount)

  ".value" - {

    val fieldName = "value"

    val validDataGeneratorForPositive =
      Gen.choose[BigDecimal](BigDecimal(0.01), maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    val validDataGeneratorForOutOfRangeNegative =
      Gen.choose[BigDecimal](minimum - 1000, minimum - 0.01)
        .map(_.setScale(2, RoundingMode.HALF_DOWN))
        .map(_.toString)

    "bind valid data positive" in {

      forAll(validDataGeneratorForPositive -> "validDataItem") {
        (dataItem: String) =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe empty
      }
    }

    behave like decimalField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "vatAmountCorrectionCountry.error.nonNumeric", Seq(country)),
      invalidNumericError = FormError(fieldName, "vatAmountCorrectionCountry.error.wholeNumber", Seq(country))
    )

    behave like decimalFieldWithMinimum(
      form,
      fieldName,
      minimum = 0,

      expectedError = FormError(fieldName, "vatAmountCorrectionCountry.error.negative", Seq(CurrencyFormatter.currencyFormat(maximumCorrectionAmount)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "vatAmountCorrectionCountry.error.required", Seq(country))
    )

    "show correct error" in {
      val form = new VatAmountCorrectionCountryFormProvider()(country, minimum)

      val result = form.bind(Map(fieldName -> (maximum + 0.01).toString)).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "vatAmountCorrectionCountry.error.outOfRange.undeclared", Seq(0, maximum)))
    }


    "show correct error when country undeclared" in {
      val form = new VatAmountCorrectionCountryFormProvider()(country, minimum)

      val result = form.bind(Map(fieldName -> (maximum + 0.01).toString)).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "vatAmountCorrectionCountry.error.outOfRange.undeclared", Seq(0, maximum)))
    }

    "fail when value is more than the original declared amount" in {

      val form = new VatAmountCorrectionCountryFormProvider()(country, maximumCorrectionAmount)

      forAll(validDataGeneratorForOutOfRangeNegative -> "validDataItem") {
        (dataItem: String) =>
          val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
          result.value.value mustBe dataItem
          result.errors mustBe List(FormError(
            fieldName,
            "vatAmountCorrectionCountry.error.negative",
            Seq(CurrencyFormatter.currencyFormat(maximumCorrectionAmount))
          ))
      }
    }
  }
}
