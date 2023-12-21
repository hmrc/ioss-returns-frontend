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

package models.financialdata

import models.Period
import models.payments.Charge
import play.api.libs.json.{Format, Json}

import java.time.ZonedDateTime

final case class FinancialData(
                                idType: Option[String],
                                idNumber: Option[String],
                                regimeType: Option[String],
                                processingDate: ZonedDateTime,
                                financialTransactions: Option[Seq[FinancialTransaction]]
                              )

object FinancialData {
  implicit val format: Format[FinancialData] = Json.format[FinancialData]

  implicit class FromFinancialDataToCharge(financialData: FinancialData) {
    def getChargeForPeriod(period: Period): Option[Charge] = {
      for {
        financialTransactions <- financialData.financialTransactions
        transactionsForPeriod <- transactionsForPeriod(financialTransactions, period)
      } yield
        Charge(
          period,
          originalAmount = transactionsForPeriod.map(_.originalAmount.getOrElse(BigDecimal(0))).sum,
          outstandingAmount = transactionsForPeriod.map(_.outstandingAmount.getOrElse(BigDecimal(0))).sum,
          clearedAmount = transactionsForPeriod.map(_.clearedAmount.getOrElse(BigDecimal(0))).sum
        )
    }

    private def transactionsForPeriod(financialTransactions: Seq[FinancialTransaction], period: Period): Option[Seq[FinancialTransaction]] = {
      val transactions = financialTransactions.filter(t => t.taxPeriodFrom.contains(period.firstDay))
      if (transactions.isEmpty) {
        None
      }
      else {
        Some(transactions)
      }
    }
  }
}
