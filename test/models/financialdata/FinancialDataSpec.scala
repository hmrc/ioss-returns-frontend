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

package models.financialdata

import base.SpecBase
import play.api.libs.json.Json

import java.time.{LocalDate, ZonedDateTime}

class FinancialDataSpec extends SpecBase {

  "FinancialData" - {

    "serialize and deserialize correctly" in {
      val financialData = FinancialData(
        idType = Some("VAT"),
        idNumber = Some(iossNumber),
        regimeType = Some("REGIME_A"),
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = Some(Seq(
          FinancialTransaction(
            chargeType = Some("CHARGE_1"),
            mainType = Some("MAIN_TYPE_1"),
            taxPeriodFrom = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
            originalAmount = Some(BigDecimal("1000.50")),
            outstandingAmount = Some(BigDecimal("200.00")),
            clearedAmount = Some(BigDecimal("800.50")),
            items = Some(Seq(
              Item(
                amount = Some(BigDecimal("500")),
                clearingReason = Some("Clearing Reason"),
                paymentReference = Some("REF123456"),
                paymentAmount = Some(BigDecimal("500")),
                paymentMethod = Some("Credit Card")
              ),
              Item(
                amount = None,
                clearingReason = Some("Reason2"),
                paymentReference = None,
                paymentAmount = Some(BigDecimal("300")),
                paymentMethod = None
              )
            ))
          )
        ))
      )

      val json = Json.toJson(financialData)
      val deserialized = json.as[FinancialData]

      deserialized mustBe financialData
    }

    "handle missing optional fields correctly" in {

      val financialData = FinancialData(
        idType = None,
        idNumber = None,
        regimeType = None,
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = None
      )

      val json = Json.toJson(financialData)
      val deserialized = json.as[FinancialData]

      deserialized mustBe financialData
    }
  }

}
