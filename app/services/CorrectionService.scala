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
import connectors.VatReturnConnector
import logging.Logging
import models.corrections.{CorrectionToCountry, PeriodWithCorrections}
import models.etmp.EtmpVatReturn
import models.requests.corrections.CorrectionRequest
import models.{Country, DataMissingError, Index, Period, StandardPeriod, UserAnswers, ValidationResult}
import pages.corrections.CorrectPreviousReturnPage
import queries.{AllCorrectionCountriesQuery, AllCorrectionPeriodsQuery, CorrectionToCountryQuery}
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class CorrectionService @Inject()(
                                   vatReturnConnector: VatReturnConnector
                                 )(implicit ec: ExecutionContext) extends Logging {

  def getAccumulativeVatForCountryTotalAmount(
                                               iossNumber: String,
                                               country: Country,
                                               correctionPeriod: Period
                                             )(implicit hc: HeaderCarrier): Future[(Boolean, BigDecimal)] = {
    for {
      returnCorrectionValue <- vatReturnConnector.getReturnCorrectionValue(iossNumber, country.code, correctionPeriod)
      correctionReturn <- vatReturnConnector.get(correctionPeriod)
    } yield {
      val isPreviouslyDeclaredCountry = correctionReturn match {
        case Right(vatReturn) =>
          vatReturn.goodsSupplied.exists(_.msOfConsumption == country.code)
        case Left(error) => throw new IllegalStateException(s"Unable to get vat return for accumulating correction total $error")
      }

      (isPreviouslyDeclaredCountry, returnCorrectionValue.maximumCorrectionValue)
    }
  }

  def fromUserAnswers(answers: UserAnswers, vrn: Vrn, period: Period): ValidationResult[CorrectionRequest] = {
    getCorrections(answers).map { corrections =>
      CorrectionRequest(vrn, StandardPeriod.fromPeriod(period), corrections)
    }
  }

  private def getCorrections(answers: UserAnswers): ValidationResult[List[PeriodWithCorrections]] = {
    answers.get(CorrectPreviousReturnPage(0)) match {
      case Some(false) =>
        List.empty[PeriodWithCorrections].validNec
      case Some(true) =>
        processCorrections(answers)
      case None =>
        DataMissingError(CorrectPreviousReturnPage(0)).invalidNec
    }
  }

  private def processCorrections(answers: UserAnswers): ValidationResult[List[PeriodWithCorrections]] = {
    answers.get(AllCorrectionPeriodsQuery) match {
      case Some(periodWithCorrections) if periodWithCorrections.nonEmpty =>
        periodWithCorrections.zipWithIndex.map {
          case (_, index) =>
            processCorrectionsToCountry(answers, Index(index))
        }.sequence.map { _ =>
          periodWithCorrections
        }
      case _ =>
        DataMissingError(AllCorrectionPeriodsQuery).invalidNec
    }
  }


  private def processCorrectionsToCountry(answers: UserAnswers, periodIndex: Index): ValidationResult[List[CorrectionToCountry]] = {
    answers.get(AllCorrectionCountriesQuery(periodIndex)) match {
      case Some(value) if value.nonEmpty =>
        value.zipWithIndex.map {
          case (_, index) =>
            processCorrectionToCountry(answers, periodIndex, Index(index))
        }.sequence
      case _ =>
        DataMissingError(AllCorrectionCountriesQuery(periodIndex)).invalidNec
    }
  }

  private def processCorrectionToCountry(answers: UserAnswers, periodIndex: Index, countryIndex: Index): ValidationResult[CorrectionToCountry] = {
    answers.get(CorrectionToCountryQuery(periodIndex, countryIndex)) match {
      case Some(value) =>
        value match {
          case CorrectionToCountry(_, Some(_)) => value.validNec
          case CorrectionToCountry(_, None) => DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)).invalidNec

        }
      case _ =>
        DataMissingError(CorrectionToCountryQuery(periodIndex, countryIndex)).invalidNec
    }
  }
}
