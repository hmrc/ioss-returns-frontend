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

import base.SpecBase
import models.Period
import models.financialdata.FinancialData._
import models.payments.Charge

import java.time.{LocalDate, Month, ZoneOffset, ZonedDateTime}

class FinancialDataSpec extends SpecBase {
  protected val zonedNow: ZonedDateTime = ZonedDateTime.of(2023, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC)
  protected val zonedDateTimeNow = ZonedDateTime.now().plusSeconds(1)
  protected val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  protected val dateTo: LocalDate = zonedNow.toLocalDate
  protected val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))

  val originalAmount1 = BigDecimal(200)
  val clearedAmount1 = BigDecimal(50)
  val outstandingAmount1 = BigDecimal(150)

  val originalAmount2 = BigDecimal(1000)
  val clearedAmount2 = BigDecimal(150)
  val outstandingAmount2 = BigDecimal(350)

  protected val financialTransaction = FinancialTransaction(Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), None, None, None, Some(Seq(item)))
  val ft1 = generateFinancialTransaction(None, Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
  val ft2 = generateFinancialTransaction(None, Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))
  val financialData = FinancialData(None, None, None, zonedDateTimeNow, Some(List(ft1, ft2)))

  private def generateFinancialTransaction(
                                            period: Option[Period],
                                            originalAmount: Option[BigDecimal],
                                            outstandingAmount: Option[BigDecimal],
                                            clearedAmount: Option[BigDecimal]
                                          ): FinancialTransaction = {
    financialTransaction
      .copy(
        taxPeriodFrom = period.map(_.firstDay).fold(financialTransaction.taxPeriodFrom)(Some(_)),
        originalAmount = originalAmount,
        outstandingAmount = outstandingAmount,
        clearedAmount = clearedAmount
      )

  }

  "FinancialData" - {

    "must not generate charge when all transactions are within given period" in {
      val period = Period(dateFrom.getYear, dateFrom.getMonth)

      financialData.getChargeForPeriod(period) mustBe Some(Charge(
        period,
        originalAmount1 + originalAmount2,
        outstandingAmount1 + outstandingAmount2,
        clearedAmount1 + clearedAmount2
      ))
    }

    "must not generate charge when all transactions are within a different period than the given period" in {
      val differentPeriodThanTransactions = Period(2020, Month.JANUARY)

      financialData.getChargeForPeriod(differentPeriodThanTransactions) mustBe None
    }

    "must not generate charge from the transactions of the given period and neglect the transactions not from the given period" in {
      val period = Period(2021, Month.MARCH)
      val otherPeriod = Period(2020, Month.MARCH)

      val ft1 = generateFinancialTransaction(Some(otherPeriod), Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
      val ft2 = generateFinancialTransaction(Some(period), Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))
      val ft3 = generateFinancialTransaction(Some(period), Some(originalAmount1), Some(outstandingAmount1), Some(clearedAmount1))
      val ft4 = generateFinancialTransaction(Some(otherPeriod), Some(originalAmount2), Some(outstandingAmount2), Some(clearedAmount2))

      financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4))).getChargeForPeriod(period) mustBe Some(Charge(
        period,
        originalAmount1 + originalAmount2,
        outstandingAmount1 + outstandingAmount2,
        clearedAmount1 + clearedAmount2
      ))
    }
  }
}
