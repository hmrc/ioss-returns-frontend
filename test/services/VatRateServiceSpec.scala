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

package services

import base.SpecBase
import models.VatRateFromCountry._
import models.VatRateType.{Reduced, Standard}
import models.{Country, Index, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.libs.json.Json
import play.api.{Configuration, Environment}

import java.io.ByteArrayInputStream

class VatRateServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val country = arbitrary[Country].sample.value
  private val countryIndex: Index = Index(0)
  private val vatRateIndex: Index = Index(0)

  ".vatRates" - {

    "must get all VAT rates for a country that were valid on or before the last day of the period" - {

      "and which have no end date" in {

        val rates: Map[String, Seq[VatRateFromCountry]] = Map(
          country.code -> Seq(
            VatRateFromCountry(BigDecimal(0), Standard, period.firstDay.minusDays(1)),
            VatRateFromCountry(BigDecimal(1), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(2), Reduced, period.lastDay),
            VatRateFromCountry(BigDecimal(3), Reduced, period.lastDay.plusDays(1))
          )
        )
        val ratesBytes = Json.toJson(rates).toString.getBytes

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.vatRates(period, country)

        result must contain theSameElementsAs Seq(
          VatRateFromCountry(BigDecimal(0), Standard, period.firstDay.minusDays(1)),
          VatRateFromCountry(BigDecimal(1), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(2), Reduced, period.lastDay)
        )
      }

      "and which have end dates that are on or after the first day of the period" in {

        val rates: Map[String, Seq[VatRateFromCountry]] = Map(
          country.code -> Seq(
            VatRateFromCountry(BigDecimal(0), Standard, period.firstDay.minusMonths(1), Some(period.firstDay)),
            VatRateFromCountry(BigDecimal(1), Reduced, period.firstDay.minusMonths(1), Some(period.firstDay.plusDays(1))),
            VatRateFromCountry(BigDecimal(2), Reduced, period.lastDay.minusMonths(1), Some(period.firstDay.minusDays(1)))
          )
        )
        val ratesBytes = Json.toJson(rates).toString.getBytes

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.vatRates(period, country)

        result must contain theSameElementsAs Seq(
          VatRateFromCountry(BigDecimal(0), Standard, period.firstDay.minusMonths(1), Some(period.firstDay)),
          VatRateFromCountry(BigDecimal(1), Reduced, period.firstDay.minusMonths(1), Some(period.firstDay.plusDays(1)))
        )
      }
    }
  }

  ".countRemainingVatRatesForCountry" - {

    "must return the remaining VAT rate(s) for a given country" - {

      "when there is only one VAT rate remaining" in {

        val rates: Map[String, Seq[VatRateFromCountry]] = Map(
          country.code -> Seq(
            VatRateFromCountry(BigDecimal(21.0), Standard, period.firstDay),
            VatRateFromCountry(BigDecimal(17.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(14.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(5.0), Reduced, period.firstDay)
          )
        )

        val ratesBytes = Json.toJson(rates).toString.getBytes

        val currentlyAnsweredVatRates: List[VatRateFromCountry] = List(
          VatRateFromCountry(BigDecimal(21.0), Standard, period.firstDay),
          VatRateFromCountry(BigDecimal(14.0), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(5.0), Reduced, period.firstDay)
        )

        val answers: UserAnswers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(countryIndex), country).success.value
          .set(VatRatesFromCountryPage(countryIndex), currentlyAnsweredVatRates).success.value
          .set(SalesToCountryPage(countryIndex, vatRateIndex), 1).success.value
          .set(VatOnSalesPage(countryIndex, vatRateIndex), VatOnSales.Option1).success.value

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.countRemainingVatRatesForCountry(countryIndex, answers)

        result must contain theSameElementsAs Seq(VatRateFromCountry(BigDecimal(17.0), Reduced, period.firstDay))
      }

      "when there are multiple VAT rates remaining" in {

        val rates: Map[String, Seq[VatRateFromCountry]] = Map(
          country.code -> Seq(
            VatRateFromCountry(BigDecimal(26.0), Standard, period.firstDay),
            VatRateFromCountry(BigDecimal(21.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(13.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(7.5), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(4.3), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(2.09), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(0.3), Reduced, period.firstDay)
          )
        )

        val ratesBytes = Json.toJson(rates).toString.getBytes

        val currentlyAnsweredVatRates: List[VatRateFromCountry] = List(
          VatRateFromCountry(BigDecimal(13.0), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(4.3), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(2.09), Reduced, period.firstDay)
        )

        val answers: UserAnswers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(countryIndex), country).success.value
          .set(VatRatesFromCountryPage(countryIndex), currentlyAnsweredVatRates).success.value
          .set(SalesToCountryPage(countryIndex, vatRateIndex), 1).success.value
          .set(VatOnSalesPage(countryIndex, vatRateIndex), VatOnSales.Option1).success.value

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.countRemainingVatRatesForCountry(countryIndex, answers)

        result must contain theSameElementsAs Seq(
          VatRateFromCountry(BigDecimal(26.0), Standard, period.firstDay),
          VatRateFromCountry(BigDecimal(21.0), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(7.5), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(0.3), Reduced, period.firstDay)
        )
      }

      "when there are no VAT rates remaining" in {

        val rates: Map[String, Seq[VatRateFromCountry]] = Map(
          country.code -> Seq(
            VatRateFromCountry(BigDecimal(21.7), Standard, period.firstDay),
            VatRateFromCountry(BigDecimal(12.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(9.4), Reduced, period.firstDay)
          )
        )

        val ratesBytes = Json.toJson(rates).toString.getBytes

        val currentlyAnsweredVatRates: List[VatRateFromCountry] = List(
          VatRateFromCountry(BigDecimal(21.7), Standard, period.firstDay),
          VatRateFromCountry(BigDecimal(12.0), Reduced, period.firstDay),
          VatRateFromCountry(BigDecimal(9.4), Reduced, period.firstDay)
        )

        val answers: UserAnswers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(countryIndex), country).success.value
          .set(VatRatesFromCountryPage(countryIndex), currentlyAnsweredVatRates).success.value
          .set(SalesToCountryPage(countryIndex, vatRateIndex), 1).success.value
          .set(VatOnSalesPage(countryIndex, vatRateIndex), VatOnSales.Option1).success.value

        val mockEnv = mock[Environment]
        val mockConfig = mock[Configuration]
        when(mockConfig.get[String](any())(any())).thenReturn("foo")
        when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))

        val service = new VatRateService(mockEnv, mockConfig)

        val result = service.countRemainingVatRatesForCountry(countryIndex, answers)

        result mustBe Seq.empty
      }
    }

    // TODO -> Different Exception caught in vatRates val
//    "must throw an exception when country can't be found" in {
//
//      val country = Country("NO", "Norway")
//
//      val rates: Map[String, Seq[VatRateFromCountry]] = Map(
//        country.code -> Seq.empty
//      )
//
//      val ratesBytes = Json.toJson(rates).toString.getBytes
//
//      val currentlyAnsweredVatRates = List.empty
//
//      val answers: UserAnswers = emptyUserAnswers
//        .set(SoldGoodsPage, true).success.value
//        .set(SoldToCountryPage(countryIndex), country).success.value
//        .set(VatRatesFromCountryPage(countryIndex), currentlyAnsweredVatRates).success.value
//        .set(SalesToCountryPage(countryIndex, vatRateIndex), 1).success.value
//        .set(VatOnSalesPage(countryIndex, vatRateIndex), VatOnSales.Option1).success.value
//
//      val mockEnv = mock[Environment]
//      val mockConfig = mock[Configuration]
//      when(mockConfig.get[String](any())(any())).thenReturn("foo")
//      when(mockEnv.resourceAsStream(any())).thenReturn(Some(new ByteArrayInputStream(ratesBytes)))
//
//      val service = new VatRateService(mockEnv, mockConfig)
//
//      val result = service.countRemainingVatRatesForCountry(countryIndex, answers)
//
//      result mustBe Seq.empty
//
////      val response = intercept[Exception](service.countRemainingVatRatesForCountry(countryIndex, answers))
//
////      response.getMessage must include("VAT rates file contained entry NO that is not recognised")
//    }
  }
}
