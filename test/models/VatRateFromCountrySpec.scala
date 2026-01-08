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

package models

import generators.{Generators, ModelGenerators}
import models.VatRateType.{Reduced, Standard}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsSuccess, Json}

import java.time.LocalDate

class VatRateFromCountrySpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues with ModelGenerators with Generators {

  "VatRatesFromCountry" - {

    "must deserialise when the rate is a JsNumber" in {

      val json = Json.obj(
        "rate" -> 1.0,
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      val expectedVatRate = VatRateFromCountry(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))
      json.validate[VatRateFromCountry] mustEqual JsSuccess(expectedVatRate)
    }

    "must deserialise when the rate is a JsString" in {

      val json = Json.obj(
        "rate" -> "1.0",
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      val expectedVatRate = VatRateFromCountry(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))
      json.validate[VatRateFromCountry] mustEqual JsSuccess(expectedVatRate)
    }

    "must serialise with the rate as a string" in {

      val vatRate = VatRateFromCountry(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1))

      val expectedJson = Json.obj(
        "rate" -> "1.0",
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      Json.toJson(vatRate) mustEqual expectedJson
    }

    "must serialise and deserialise when validUntil is present" in {

      val vatRate = VatRateFromCountry(BigDecimal(1.0), Standard, LocalDate.of(2021, 7, 1), Some(LocalDate.of(2022, 1, 1)))

      Json.toJson(vatRate).validate[VatRateFromCountry] mustEqual JsSuccess(vatRate)
    }

    "must serialize and deserialize when validUntil is None" in {
      val vatRate = VatRateFromCountry(BigDecimal(15), Reduced, LocalDate.of(2023, 1, 1), None)

      Json.toJson(vatRate).validate[VatRateFromCountry] mustEqual JsSuccess(vatRate)
    }

    "must fail to deserialize when invalid data" in {
      val json = Json.obj(
        "rate" -> "1.0",
        "rateType" -> 12345,
        "validFrom" -> "2021-07-01"
      )

      json.validate[VatRateFromCountry] mustBe a[JsError]
    }

    "must fail to deserialize when rate is missing" in {
      val json = Json.obj(
        "rateType" -> Standard.toString,
        "validFrom" -> "2021-07-01"
      )

      json.validate[VatRateFromCountry] mustBe a[JsError]
    }
  }

  ".asPercentage" - {

    "must return the rate divided by 100" in {

      forAll(arbitrary[VatRateFromCountry]) {
        vatRate =>
          vatRate.asPercentage mustEqual vatRate.rate / 100
      }
    }

    "must return zero when rate is zero" in {
      val vatRate = VatRateFromCountry(BigDecimal(0), Standard, LocalDate.of(2023, 1, 1))
      vatRate.asPercentage mustEqual BigDecimal(0)
    }

    "must return negative value when rate is negative" in {
      val vatRate = VatRateFromCountry(BigDecimal(-10), Reduced, LocalDate.of(2023, 1, 1))
      vatRate.asPercentage mustEqual BigDecimal(-0.1)
    }
  }

  ".rateForDisplay" - {

    "must return the rate as a whole number string with '%' when the rate is a whole number" in {
      val vatRate = VatRateFromCountry(BigDecimal(20), Standard, LocalDate.of(2021, 7, 1))
      vatRate.rateForDisplay mustEqual "20%"
    }

    "must return the rate as a decimal string with '%' when the rate is not a whole number" in {
      val vatRate = VatRateFromCountry(BigDecimal(20.5), Standard, LocalDate.of(2021, 7, 1))
      vatRate.rateForDisplay mustEqual "20.5%"
    }
  }

  "fromEuVatRate" - {
    "must correctly create a VatRateFromCountry from an EuVatRate" in {
      val euVatRate = EuVatRate(
        country = Country("DE", "Germany"),
        vatRate = BigDecimal(5),
        vatRateType = Reduced,
        situatedOn = LocalDate.of(2022, 5, 1)
      )

      val expectedVatRate = VatRateFromCountry(
        rate = BigDecimal(5),
        rateType = Reduced,
        validFrom = LocalDate.of(2022, 5, 1),
        validUntil = None
      )

      VatRateFromCountry.fromEuVatRate(euVatRate) mustEqual expectedVatRate
    }

    "must handle zero or negative rates correctly" in {
      val zeroRate = VatRateFromCountry(BigDecimal(0), Standard, LocalDate.of(2023, 1, 1))
      zeroRate.rateForDisplay mustEqual "0%"

      val negativeRate = VatRateFromCountry(BigDecimal(-5), Reduced, LocalDate.of(2023, 1, 1))
      negativeRate.rateForDisplay mustEqual "-5%"
    }

    "must serialize and deserialize when rate is zero" in {
      val vatRate = VatRateFromCountry(BigDecimal(0), Standard, LocalDate.of(2023, 1, 1))

      Json.toJson(vatRate).validate[VatRateFromCountry] mustEqual JsSuccess(vatRate)
    }

    "must fail to deserialize when rateType is invalid" in {
      val json = Json.obj(
        "rate" -> "10",
        "rateType" -> "INVALID",
        "validFrom" -> "2021-07-01"
      )

      json.validate[VatRateFromCountry] mustBe a[JsError]
    }
  }
}
