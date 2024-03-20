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

package services

import base.SpecBase
import models.VatOnSalesChoice.Standard
import models.{Country, Index, TotalVatToCountry, VatOnSales}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage}
import pages.{SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}

class SalesAtVatRateServiceSpec extends SpecBase with MockitoSugar {

  private val index0 = Index(0)
  private val index1 = Index(1)

  private val belgium: Country = Country("BE", "Belgium")
  private val croatia: Country = Country("HR", "Croatia")
  private val estonia: Country = Country("EE", "Estonia")
  private val spain: Country = Country("ES", "Spain")

  val service = new SalesAtVatRateService()

  "SalesAtVatRateService" - {

    "getTotalVatOnSales" - {

      "must show correct total vat from one country, to one country, with one vat rate" in {

        service.getTotalVatOnSales(completeUserAnswers) mustBe Some(BigDecimal(20))
      }

      "must show correct total vat from one country with multiple vat rates" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), croatia).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(10))).success.value

        service.getTotalVatOnSales(answers) mustBe Some(BigDecimal(30))
      }

      "must show correct total vat from multiple countries with multiple vat rates" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index0), croatia).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(10))).success.value
          .set(SoldToCountryPage(index1), estonia).success.value
          .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index1, index0), BigDecimal(300)).success.value
          .set(VatOnSalesPage(index1, index0), VatOnSales(Standard, BigDecimal(60))).success.value
          .set(SalesToCountryPage(index1, index1), BigDecimal(400)).success.value
          .set(VatOnSalesPage(index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getTotalVatOnSales(answers) mustBe Some(BigDecimal(110))
      }
    }

    "getTotalNetSales" - {

      "must show correct net total sales for one country from with one vat rate" in {

        service.getTotalNetSales(completeUserAnswers) mustBe Some(BigDecimal(100))
      }

      "must show correct net total sales for one country from with multiple vat rates" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), croatia).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getTotalNetSales(answers) mustBe Some(BigDecimal(300))
      }

      "must show correct net total sales for multiple country from with multiple vat rates" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index0), croatia).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SoldToCountryPage(index1), estonia).success.value
          .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index1, index0), BigDecimal(300)).success.value
          .set(VatOnSalesPage(index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index1, index1), BigDecimal(400)).success.value
          .set(VatOnSalesPage(index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getTotalNetSales(answers) mustBe Some(BigDecimal(1000))
      }
    }

    "getVatOwedToCountries" - {

      "when the corrections exist" - {

        "must return correct total vat to eu countries for one country with one vat rate" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), belgium).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country with one vat rate and a correction for another country" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), spain).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(100)).success.value

          val expected = List(
            TotalVatToCountry(belgium, BigDecimal(100)),
            TotalVatToCountry(spain, BigDecimal(20))
          )

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country with multiple vat rates" in {

          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(croatia, BigDecimal(40)))

          service.getVatOwedToCountries(answers) mustBe expected
        }

        "must return correct total vat to eu countries for multiple countries with multiple vat rates" in {

          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SoldToCountryPage(index1), spain).success.value
            .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index1, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index1, index1), VatOnSales(Standard, BigDecimal(40))).success.value

          service.getVatOwedToCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(spain, BigDecimal(60)),
            TotalVatToCountry(croatia, BigDecimal(40))
          )
        }

        "must return correct total vat to eu countries for multiple countries with multiple vat rates and multiple corrections" in {

          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SoldToCountryPage(index1), spain).success.value
            .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index1, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index1, index1), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(CorrectionCountryPage(index0, index0), spain).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(-50)).success.value


          service.getVatOwedToCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(croatia, BigDecimal(40)),
            TotalVatToCountry(spain, BigDecimal(10))
          )
        }
      }

      "when the corrections is empty" - {

        "must return correct total vat to eu countries for one country with one vat rate" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), belgium).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country with multiple vat rates" in {

          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), belgium).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(40)))

          service.getVatOwedToCountries(answers) mustBe expected
        }
      }
    }

    "getTotalVatOwedAfterCorrections" - {

      "when corrections exist" - {

        "must return correct total when there is a positive correction " in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(120)

        }

        "must return correct total when there is a negative correction" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(-100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(200)
        }

        "must return zero when the correction makes the total amount negative for a country" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), croatia).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(0)
        }

        "must not subtract the negative amount for one country from the positive total for other countries" in {

          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), croatia).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), estonia).success.value
            .set(VatAmountCorrectionCountryPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(100)
        }
      }
    }
  }
}
