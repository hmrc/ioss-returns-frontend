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

import cats.data.OptionT
import models.{Index, TotalVatToCountry, UserAnswers}
import queries.{AllCorrectionPeriodsQuery, AllSalesByCountryQuery, AllSalesFromEuQuery, AllSalesFromEuQueryWithOptionalVatQuery, AllSalesQuery, AllVatRatesFromCountryQuery, SalesByCountryQuery}

import javax.inject.Inject

class SalesAtVatRateService @Inject()() {

  def getNiTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    ???
//    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
//      allSales.map(saleFromNi =>
//        saleFromNi.vatRates
//          .map(_.flatMap(_.sales.map(_.netValueOfSales)).sum).getOrElse(BigDecimal(0))
//      ).sum
//    )
  }

  def getNiTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    ???
//    userAnswers.get(AllSalesFromNiQuery).map(allSales =>
//      allSales.map(saleFromNi =>
//        saleFromNi.vatRates
//          .map(_.flatMap(_.sales.flatMap(_.vatOnSales.map(_.amount))).sum).getOrElse(BigDecimal(0))
//      ).sum
//    )
  }

  def getEuTotalNetSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesQuery).map( allSales =>
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

  def getEuTotalVatOnSales(userAnswers: UserAnswers): Option[BigDecimal] = {
    userAnswers.get(AllSalesQuery).map(allSales =>
      allSales.flatMap(
        _.vatRatesFromCountry.toSeq.flatten.map { vatRateFromCountry =>
          (for {
            salesAtVatRate <- vatRateFromCountry.salesAtVatRate
            vatOnSales <- salesAtVatRate.vatOnSales
          } yield vatOnSales.amount).getOrElse(BigDecimal(0))
        }
      ).sum
    )
  }

  def getTotalVatOwedAfterCorrections(userAnswers: UserAnswers): BigDecimal =
    getVatOwedToEuCountries(userAnswers).filter(vat => vat.totalVat > 0).map(_.totalVat).sum

  def getVatOwedToEuCountries(userAnswers: UserAnswers): List[TotalVatToCountry] = {
    val vatOwedToEuCountriesFromEu =
      for {
        allSalesFromEu <- userAnswers.get(AllSalesQuery).toList.flatten.zipWithIndex
        salesFromCountry = allSalesFromEu._1.country
        salesToCountry <- userAnswers.get(AllSalesByCountryQuery(Index(allSalesFromEu._2))).toSeq
        vatRate <- salesToCountry.vatRatesFromCountry.toSeq.flatten
      } yield TotalVatToCountry(salesFromCountry, vatRate.salesAtVatRate.flatMap(_.vatOnSales.map(_.amount)).getOrElse(BigDecimal(0)))

    val correctionCountriesAmount =
      for {
        allCorrectionPeriods <- userAnswers.get(AllCorrectionPeriodsQuery).toSeq
        periodWithCorrections <- allCorrectionPeriods
        countryCorrection <- periodWithCorrections.correctionsToCountry.getOrElse(List.empty).filter(_.countryVatCorrection.isDefined)
      } yield TotalVatToCountry(countryCorrection.correctionCountry, countryCorrection.countryVatCorrection.getOrElse(BigDecimal(0)))

    val vatOwedToEuCountries =
      vatOwedToEuCountriesFromEu ++  correctionCountriesAmount

    vatOwedToEuCountries.groupBy(_.country).map {
      case (country, salesToCountry) =>
        val totalVatToCountry = salesToCountry.map(_.totalVat).sum
        TotalVatToCountry(country, totalVatToCountry)
    }.toList
  }
}
