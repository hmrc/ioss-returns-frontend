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

package utils

import models.{Country, Index}
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Result}
import queries._

import scala.concurrent.Future

trait CompletionChecks {

  protected def withCompleteData[A](index: Index, data: Index => Seq[A], onFailure: Seq[A] => Result)
                                   (onSuccess: => Result): Result = {
    val incomplete = data(index)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](index: Index, data: Index => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {

    val incomplete = data(index)
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }


  protected def withCompleteData[A](data: () => Seq[A], onFailure: Seq[A] => Result)
                                   (onSuccess: => Result): Result = {

    val incomplete = data()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  protected def withCompleteDataAsync[A](data: () => Seq[A], onFailure: Seq[A] => Future[Result])
                                        (onSuccess: => Future[Result]): Future[Result] = {

    val incomplete = data()
    if(incomplete.isEmpty) {
      onSuccess
    } else {
      onFailure(incomplete)
    }
  }

  //corrections
  def getIncompleteCorrections(periodIndex: Index)(implicit request: DataRequest[AnyContent]): List[CorrectionToCountry] = {
    request.userAnswers
      .get(AllCorrectionCountriesQuery(periodIndex))
      .map(_.filter(_.countryVatCorrection.isEmpty)).getOrElse(List.empty)
  }

  def firstIndexedIncompleteCorrection(periodIndex: Index, incompleteCorrections: Seq[CorrectionToCountry])
                                      (implicit request: DataRequest[AnyContent]): Option[(CorrectionToCountry, Int)] = {
    request.userAnswers.get(AllCorrectionCountriesQuery(periodIndex))
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCorrections.contains(indexedCorrection._1))
  }

  //check-sales
  def getIncompleteVatRateAndSales(countryIndex: Index)(implicit request: DataRequest[AnyContent]): Seq[VatRateWithOptionalSalesFromCountry] = {
      val noSales = request.userAnswers
        .get(AllSalesWithOptionalVatQuery(countryIndex))
        .map(_.filter(_.salesAtVatRate.isEmpty)).getOrElse(List.empty)

    val noVat = request.userAnswers
      .get(AllSalesWithOptionalVatQuery(countryIndex))
      .map(
        _.filter( v =>
          v.salesAtVatRate.exists(_.vatOnSales.isEmpty)
        )
      ).getOrElse(List.empty)

    noSales ++ noVat
  }

  //sold to country list controller
  def getCountriesWithIncompleteSales()(implicit request: DataRequest[AnyContent]): Seq[Country] = {
    request.userAnswers
      .get(AllSalesWithTotalAndVatQuery)
      .map(_.filter(sales =>
        sales.vatRatesFromCountry.isEmpty ||
          sales.vatRatesFromCountry.getOrElse(List.empty).exists(
            rate => rate.salesAtVatRate.isEmpty || rate.salesAtVatRate.exists(_.vatOnSales.isEmpty)
          )
      ))
      .map(_.map(_.country))
      .getOrElse(Seq.empty)
  }

  def firstIndexedIncompleteCountrySales(incompleteCountries: Seq[Country])
                                        (implicit request: DataRequest[AnyContent]): Option[(SalesToCountryWithOptionalSales, Int)] = {
    request.userAnswers.get(AllSalesWithTotalAndVatQuery)
      .getOrElse(List.empty).zipWithIndex
      .find(indexedCorrection => incompleteCountries.contains(indexedCorrection._1.country))
  }

}
