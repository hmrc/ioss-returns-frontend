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

package viewmodels.previousReturns

import models.Country.getCountryName
import models.etmp.{EtmpVatReturn, EtmpVatReturnCorrection}
import utils.{ConvertPeriodKey, CurrencyFormatter}

object PreviousReturnsCorrectionsSummary {

  case class CorrectionRow(
                            period: String,
                            country: String,
                            totalVATAmountCorrectionGBP: String,
                            isFirst: Boolean,
                            isLastCountry: Boolean,
                            isLastPeriod: Boolean
                          )

  def correctionRows(etmpVatReturn: EtmpVatReturn): Seq[CorrectionRow] = {
    val correctionsGroupedByPeriod = etmpVatReturn.correctionPreviousVATReturn.groupBy(_.periodKey).toSeq.sortBy(_._1)
    correctionsGroupedByPeriod.zipWithIndex.flatMap {
      case ((periodKey, correctionPeriodVatReturns), periodIndex) =>
        correctionPeriodVatReturns.zipWithIndex.map {
          case (correctionPeriodVatReturn, index) if index == 0 =>
            val isFirst = true
            val isLastCountry = correctionPeriodVatReturns.size == index + 1
            val isLastPeriod = correctionsGroupedByPeriod.size == periodIndex + 1

            getCorrectionRow(periodKey, correctionPeriodVatReturn, isFirst, isLastCountry, isLastPeriod)
          case (correctionPeriodVatReturn, index) =>
            val isFirst = false
            val isLastCountry = correctionPeriodVatReturns.size == index + 1

            val isLastPeriod = correctionsGroupedByPeriod.size == periodIndex + 1

            getCorrectionRow(periodKey, correctionPeriodVatReturn, isFirst, isLastCountry, isLastPeriod)
        }
    }
  }

  private def getCorrectionRow(
                                periodKey: String,
                                correctionPeriodVatReturn: EtmpVatReturnCorrection,
                                isFirst: Boolean,
                                isLastCountry: Boolean,
                                isLastPeriod: Boolean
                              ): CorrectionRow = {
    val month = ConvertPeriodKey.monthNameFromEtmpPeriodKey(periodKey)
    val year = ConvertPeriodKey.yearFromEtmpPeriodKey(periodKey)
    val periodString = s"$month $year"
    val country = getCountryName(correctionPeriodVatReturn.msOfConsumption)

    CorrectionRow(
      periodString,
      country,
      CurrencyFormatter.currencyFormat(correctionPeriodVatReturn.totalVATAmountCorrectionGBP),
      isFirst,
      isLastCountry,
      isLastPeriod
    )
  }
}
