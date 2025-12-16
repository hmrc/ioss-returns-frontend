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

import cats.implicits.{catsSyntaxValidatedIdBinCompat0, toTraverseOps}
import connectors.ReturnStatusConnector
import logging.Logging
import models.requests.{RegistrationRequest, VatReturnRequest}
import models.{DataMissingError, Index, Period, StandardPeriod, SubmissionStatus, UserAnswers, ValidationResult, VatRateFromCountry, VatRateType}
import models.domain.{VatRate as DomainVatRate, VatRateType as DomainVatRateType}
import pages.{SoldGoodsPage, VatRatesFromCountryPage}
import queries.{AllSalesQuery, OptionalSalesAtVatRate, SalesAtVatRateQuery, SalesDetails, SalesToCountry, VatOnSalesFromQuery}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.yourAccount.Return

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnService @Inject()(
                                  returnStatusConnector: ReturnStatusConnector
                                )(implicit ec: ExecutionContext) extends Logging {

  def fromUserAnswers(answers: UserAnswers, vrn: Option[Vrn], period: Period): ValidationResult[VatReturnRequest] = {
    getSales(answers).map(sales =>
      VatReturnRequest(vrn, StandardPeriod.fromPeriod(period), Some(period.firstDay), Some(period.lastDay), sales)
    )
  }

  def getOldestDueReturn(iossNumber: String)(implicit hc: HeaderCarrier): Future[Option[Return]] = {
    returnStatusConnector.getCurrentReturns(iossNumber).map {
      case Right(currentReturns) =>
        currentReturns
          .returns
          .filter(r => r.submissionStatus == SubmissionStatus.Due || r.submissionStatus == SubmissionStatus.Overdue)
          .sortBy(_.dueDate)
          .headOption
      case Left(error) =>
        val message = s"Error when getting current returns ${error.body}"
        logger.error(message)
        throw new Exception(message)
    }
  }

  private def getSales(answers: UserAnswers): ValidationResult[List[SalesToCountry]] =
    answers.get(SoldGoodsPage) match {
      case Some(true) =>
        processSales(answers)
      case Some(false) =>
        List.empty[SalesToCountry].validNec
      case None =>
        DataMissingError(SoldGoodsPage).invalidNec
    }

  private def processSales(answers: UserAnswers): ValidationResult[List[SalesToCountry]] = {
    answers.get(AllSalesQuery) match {
      case Some(sales) if sales.nonEmpty =>
        sales.zipWithIndex.map {
          case (_, index) =>
            processSalesToCountry(answers, Index(index), Index(index))
        }.sequence.map {
          salesDetails =>
            sales.zip(salesDetails).map {
              case (sales, salesDetails) =>
                SalesToCountry(sales.country, salesDetails)
            }
        }
      case _ =>
        DataMissingError(AllSalesQuery).invalidNec
    }
  }

  private def processSalesToCountry(answers: UserAnswers, countryIndex: Index, vatRateIndex: Index): ValidationResult[List[SalesDetails]] =
    answers.get(VatRatesFromCountryPage(countryIndex, vatRateIndex)) match {
      case Some(list) if list.nonEmpty =>
        list.zipWithIndex.map {
          case (vatRate, index) =>
            processSalesAtVatRate(answers, countryIndex, Index(index), vatRate)
        }.sequence
      case _ =>
        DataMissingError(VatRatesFromCountryPage(countryIndex, vatRateIndex)).invalidNec
    }

  private def processSalesAtVatRate(
                                     answers: UserAnswers,
                                     countryIndex: Index,
                                     vatRateIndex: Index,
                                     vatRate: VatRateFromCountry
                                   ): ValidationResult[SalesDetails] =
    answers.get(SalesAtVatRateQuery(countryIndex, vatRateIndex)) match {
      case Some(OptionalSalesAtVatRate(netValueOfSales, Some(vatOnSales))) =>
        SalesDetails(
          vatRate = toDomainVatRate(vatRate),
          netValueOfSales = netValueOfSales,
          vatOnSales = vatOnSales
        ).validNec
      case Some(OptionalSalesAtVatRate(_, None)) =>
        DataMissingError(VatOnSalesFromQuery(countryIndex, vatRateIndex)).invalidNec
      case None =>
        DataMissingError(SalesAtVatRateQuery(countryIndex, vatRateIndex)).invalidNec
    }

  private def toDomainVatRate(vatRate: VatRateFromCountry): DomainVatRate = {
    DomainVatRate(
      vatRate.rate,
      if (vatRate.rateType == VatRateType.Reduced) {
        DomainVatRateType.Reduced
      } else {
        DomainVatRateType.Standard
      }
    )
  }

}
