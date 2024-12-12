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

import java.time.LocalDate

class FinancialTransactionSpec extends SpecBase {

  "FinancialTransaction" - {

    "serialize and deserialize correctly" in {

      val financialTransaction = FinancialTransaction(
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
            )
          ))
        )

        val json = Json.toJson(financialTransaction)
        val deserialized = json.as[FinancialTransaction]

        deserialized mustBe financialTransaction
    }

    "handle missing optional fields correctly" in {
      val financialTransaction = FinancialTransaction(
        chargeType = None,
        mainType = None,
        taxPeriodFrom = None,
        taxPeriodTo = None,
        originalAmount = None,
        outstandingAmount = None,
        clearedAmount = None,
        items = None
      )

      val json = Json.toJson(financialTransaction)
      val deserialized = json.as[FinancialTransaction]

      deserialized mustBe financialTransaction
    }

  }

}
