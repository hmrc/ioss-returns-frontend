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

import models.{Index, TotalVatToCountry, UserAnswers}
import queries.{AllCorrectionPeriodsQuery, AllSalesByCountryQuery, AllSalesQuery, FooAllSalesQuery}

import javax.inject.Inject

class SalesAtVatRateService @Inject()() {

  def getTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(FooAllSalesQuery).map( allSales =>
      allSales.flatMap(
        _.vatRatesFromCountry.toSeq.flatten.map { vatRateFromCountry =>
          (for {
            salesAtVatRate <- vatRateFromCountry.salesAtVatRate
            netValueOfSales <- salesAtVatRate.netValueOfSales
          } yield netValueOfSales).getOrElse(BigDecimal(0))
        }
      ).sum
    )
  }

  def getTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(FooAllSalesQuery).map { allSales =>
      allSales.flatMap { x =>
        println("===== country: " + x.country.toString)
        x.vatRatesFromCountry.toSeq.flatten.map { vatRateFromCountry =>
          (for {
            salesAtVatRate <- vatRateFromCountry.salesAtVatRate
            vatOnSales <- salesAtVatRate.vatOnSales
          } yield {
            println ("===== amount: " + vatOnSales.amount)
            vatOnSales.amount
          }).getOrElse(BigDecimal(0))
        }
      }.sum
  }
  }

  def getTotalVatOwedAfterCorrections(userAnswers: UserAnswers): BigDecimal =
    getVatOwedToCountries(userAnswers).filter(vat => vat.totalVat > 0).map(_.totalVat).sum

  def getVatOwedToCountries(userAnswers: UserAnswers): List[TotalVatToCountry] = {
    val vatOwedToCountriesFromEu =
      for {
        allSales <- userAnswers.get(FooAllSalesQuery).toList.flatten.zipWithIndex
        salesFromCountry = allSales._1.country
        salesToCountry <- userAnswers.get(AllSalesByCountryQuery(Index(allSales._2))).toSeq
        vatRate <- salesToCountry.vatRatesFromCountry.toSeq.flatten
      } yield TotalVatToCountry(salesFromCountry, vatRate.salesAtVatRate.flatMap(_.vatOnSales.map(_.amount)).getOrElse(BigDecimal(0)))

    val correctionCountriesAmount =
      for {
        allCorrectionPeriods <- userAnswers.get(AllCorrectionPeriodsQuery).toSeq
        periodWithCorrections <- allCorrectionPeriods
        countryCorrection <- periodWithCorrections.correctionsToCountry.getOrElse(List.empty).filter(_.countryVatCorrection.isDefined)
      } yield TotalVatToCountry(countryCorrection.correctionCountry, countryCorrection.countryVatCorrection.getOrElse(BigDecimal(0)))

    val vatOwedToCountries =
      vatOwedToCountriesFromEu ++  correctionCountriesAmount

    vatOwedToCountries.groupBy(_.country).map {
      case (country, salesToCountry) =>
        val totalVatToCountry = salesToCountry.map(_.totalVat).sum
        TotalVatToCountry(country, totalVatToCountry)
    }.toList
  }
}
