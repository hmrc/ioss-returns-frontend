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
import models.VatOnSalesChoice.Standard
import models.{Country, Index, TotalVatToCountry, VatOnSales, VatOnSalesChoice, VatRateType}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionCountryPage
import pages.{SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.libs.json.Json

class SalesAtVatRateServiceSpec extends SpecBase with MockitoSugar {

  private val index0 = Index(0)
  private val index1 = Index(1)


  val service = new SalesAtVatRateService()

  "SalesAtVatRateService" - {

    "getTotalVatOnSales" - {

      "must show correct total vat from one country, to one country, with one vat rate" in {
        service.getTotalVatOnSales(completeUserAnswers) mustBe Some(BigDecimal(20))
      }

      "must show correct total vat from one country with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), Country("HR", "Croatia")).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(10))).success.value

        service.getTotalVatOnSales(answers) mustBe Some(BigDecimal(30))
      }

      "must show correct total vat from multiple countries with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsPage,true).success.value
          .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
          .set(SoldToCountryPage(index1), Country("EE", "Estonia")).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(10))).success.value
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
          .set(SoldToCountryPage(index), Country("HR", "Croatia")).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getTotalNetSales(answers) mustBe Some(BigDecimal(300))
      }

      "must show correct net total sales for multiple country from with multiple vat rates" in {
        val answers = emptyUserAnswers
          .set(SoldGoodsPage,true).success.value
          .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
          .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
          .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
          .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SoldToCountryPage(index1), Country("EE", "Estonia")).success.value
          .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
          .set(SalesToCountryPage(index1, index0), BigDecimal(300)).success.value
          .set(VatOnSalesPage(index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
          .set(SalesToCountryPage(index1, index1), BigDecimal(400)).success.value
          .set(VatOnSalesPage(index1, index1), VatOnSales(Standard, BigDecimal(20))).success.value

        service.getTotalNetSales(answers) mustBe Some(BigDecimal(1000))
      }
    }

    "getVatOwedToEuCountries" - {
      val belgium: Country = Country("BE", "Belgium")
      val denmark: Country = Country("DK", "Denmark")
      val spain: Country = Country("ES", "Spain")
/*
      "when the corrections exist" - {

        "must return correct total vat to eu countries for one country with one vat rate" in {
          val ua = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with one vat rate and a correction for the country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), belgium).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(100)).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(120)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with one vat rate and a correction for another country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), spain).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-100)).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)), TotalVatToCountry(spain, BigDecimal(-100)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, one country to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(40)))

          service.getVatOwedToCountries(answers) mustBe expected
        }

        "must return correct total vat to eu countries for one country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index0, index1), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(VatRatesFromCountryPage(index0, index1), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index1, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(40)),
            TotalVatToCountry(denmark, BigDecimal(20))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            //countries from
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(SoldToCountryPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(SalesToCountryPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(SalesToCountryPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(SalesToCountryPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(SalesToCountryPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160))
          )
        }

        "must return correct total vat to eu countries for multiple country from, multiple countries to with multiple vat rates with NI sales and Eu sales" in {
          
          val answers = completeSalesFromNIUserAnswers
            .set(SoldGoodsPage,true).success.value
            //countries from
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(SoldToCountryPage(index1), Country("EE", "Estonia")).success.value

            //countries to
            .set(CountryOfConsumptionFromEuPage(index0, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index1), denmark).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index0), belgium).success.value
            .set(CountryOfConsumptionFromEuPage(index1, index1), denmark).success.value

            //vat rates
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index0, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index1, index0), List(twentyPercentVatRate)).success.value
            .set(VatRatesFromCountryPage(index1, index1), List(twentyPercentVatRate, fivePercentVatRate)).success.value

            //sales at vat rate
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(10))).success.value
            .set(SalesToCountryPage(index0, index1, index0), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1, index1), VatOnSales(Standard, BigDecimal(30))).success.value
            .set(SalesToCountryPage(index1, index0, index0), BigDecimal(300)).success.value
            .set(VatOnSalesPage(index1, index0, index0), VatOnSales(Standard, BigDecimal(40))).success.value
            .set(SalesToCountryPage(index1, index1, index0), BigDecimal(400)).success.value
            .set(VatOnSalesPage(index1, index1, index0), VatOnSales(Standard, BigDecimal(50))).success.value
            .set(SalesToCountryPage(index1, index1, index1), BigDecimal(400)).success.value
            .set(VatOnSalesPage(index1, index1, index1), VatOnSales(Standard, BigDecimal(60))).success.value

          service.getVatOwedToEuCountries(answers) must contain theSameElementsAs List(
            TotalVatToCountry(belgium, BigDecimal(50)),
            TotalVatToCountry(denmark, BigDecimal(160)),
            TotalVatToCountry(spain, BigDecimal(1000))
          )
        }

      }
  */
      "when the corrections is empty" - {

        "must return correct total vat to eu countries for one country with one vat rate" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(20)))

          service.getVatOwedToCountries(ua) mustBe expected
        }

        "must return correct total vat to eu countries for one country with multiple vat rates" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage,true).success.value
            .set(SoldToCountryPage(index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate, fivePercentVatRate)). success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(SalesToCountryPage(index0, index1), BigDecimal(200)).success.value
            .set(VatOnSalesPage(index0, index1), VatOnSales(Standard, BigDecimal(20))).success.value

          val expected = List(TotalVatToCountry(belgium, BigDecimal(40)))

          service.getVatOwedToCountries(answers) mustBe expected
        }
      }

    }

// Will uncomment these after the correction ticket is done

    "getTotalVatOwedAfterCorrections" - {

      // remove the ones that split out EU vs NI either sales exists or they don't and either corrections exist or they don't
      "when corrections exist" - {
        // remove
        "must return correct total when NI and EU sales exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeUserAnswers) mustBe BigDecimal(1020)
        }
        // remove
        "must return zero when total NI and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(emptyUserAnswers) mustBe BigDecimal(0)
        }
        // remove
        "must return total when NI exists and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeUserAnswers) mustBe BigDecimal(1000)
        }
        // remove
        "must return total when NI doesn't exist and EU does exist" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getTotalVatOwedAfterCorrections(answers) mustBe BigDecimal(20)
        }

        "must return correct total when there is a positive correction " in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(120)

        }

        "must return correct total when there is a negative correction" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(200))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-100)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(100)
        }

        "must return zero when the correction makes the total amount negative for a country" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(0)
        }

        "must not subtract the negative amount for one country from the positive total for other countries" in {
          
          val ua = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(CountryOfConsumptionFromEuPage(index0, index0), Country("BE", "Belgium")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0, index0), BigDecimal(1000)).success.value
            .set(VatOnSalesPage(index0, index0, index0), VatOnSales(Standard, BigDecimal(100))).success.value
            .set(CorrectionReturnPeriodPage(index0), period).success.value
            .set(CorrectionCountryPage(index0, index0), Country("EE", "Estonia")).success.value
            .set(CountryVatCorrectionPage(index0, index0), BigDecimal(-1000)).success.value

          service.getTotalVatOwedAfterCorrections(ua) mustBe BigDecimal(100)
        }

      }

      "when corrections is empty" - {
        // remove
        "must return correct total when NI and EU sales exist" in {

          val answers = completeUserAnswers
            .set(SalesToCountryPage(index1, index), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index1, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(1000))).success.value

          service.getTotalVatOwedAfterCorrections(answers) mustBe BigDecimal(1020)
        }
        // remove
        "must return zero when no countries exist" in {
          
          service.getTotalVatOwedAfterCorrections(emptyUserAnswers) mustBe BigDecimal(0)
        }
        // remove
        "must return total when NI exists and EU sales don't exist" in {
          
          service.getTotalVatOwedAfterCorrections(completeSalesFromNIUserAnswers) mustBe BigDecimal(1000)
        }
        // remove
        "must return total when NI doesn't exist and EU does exist" in {
          
          val answers = emptyUserAnswers
            .set(SoldGoodsPage, true).success.value
            .set(SoldToCountryPage(index0), Country("HR", "Croatia")).success.value
            .set(VatRatesFromCountryPage(index0, index0), List(twentyPercentVatRate)).success.value
            .set(SalesToCountryPage(index0, index0), BigDecimal(100)).success.value
            .set(VatOnSalesPage(index0, index0), VatOnSales(Standard, BigDecimal(20))).success.value

          service.getTotalVatOwedAfterCorrections(answers) mustBe BigDecimal(20)
        }


      }


    }


  }
}
