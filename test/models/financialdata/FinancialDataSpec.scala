/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{JsError, JsNull, Json}

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

    "handle empty financial transactions list correctly" in {
      val financialData = FinancialData(
        idType = Some("VAT"),
        idNumber = Some("123456789"),
        regimeType = Some("REGIME_A"),
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = Some(Seq.empty)
      )

      val json = Json.toJson(financialData)
      val deserialized = json.as[FinancialData]

      deserialized mustBe financialData
    }

    "handle partially populated financial transactions correctly" in {
      val financialData = FinancialData(
        idType = Some("VAT"),
        idNumber = Some("123456789"),
        regimeType = Some("REGIME_B"),
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = Some(Seq(
          FinancialTransaction(
            chargeType = None,
            mainType = Some("MAIN_TYPE_2"),
            taxPeriodFrom = None,
            taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
            originalAmount = None,
            outstandingAmount = Some(BigDecimal("300.00")),
            clearedAmount = None,
            items = Some(Seq(
              Item(
                amount = None,
                clearingReason = Some("Clearing Reason"),
                paymentReference = None,
                paymentAmount = Some(BigDecimal("300")),
                paymentMethod = Some("Debit Card")
              )
            ))
          )
        ))
      )

      val json = Json.toJson(financialData)
      val deserialized = json.as[FinancialData]

      deserialized mustBe financialData
    }

    "fail to deserialize when JSON has invalid data" in {
      val invalidJson = Json.obj(
        "idType" -> "VAT",
        "idNumber" -> "123456789",
        "regimeType" -> "REGIME_A",
        "processingDate" -> "invalid-date",
        "financialTransactions" -> Json.arr(
          Json.obj(
            "chargeType" -> "CHARGE_1",
            "mainType" -> "MAIN_TYPE_1",
            "taxPeriodFrom" -> "2023-01-01",
            "taxPeriodTo" -> "2023-12-31",
            "originalAmount" -> "not-a-number",
            "outstandingAmount" -> "200.00",
            "clearedAmount" -> "800.50",
            "items" -> Json.arr(
              Json.obj(
                "amount" -> "500",
                "clearingReason" -> "Clearing Reason",
                "paymentReference" -> "REF123456",
                "paymentAmount" -> "500",
                "paymentMethod" -> "Credit Card"
              )
            )
          )
        )
      )

      invalidJson.validate[FinancialData] mustBe a[JsError]
    }

    "handle null optional fields correctly" in {
      val json = Json.obj(
        "idType" -> JsNull,
        "idNumber" -> JsNull,
        "regimeType" -> JsNull,
        "processingDate" -> "2024-12-12T12:00:00+00:00",
        "financialTransactions" -> JsNull
      )

      val expectedFinancialData = FinancialData(
        idType = None,
        idNumber = None,
        regimeType = None,
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = None
      )

      json.as[FinancialData] mustBe expectedFinancialData
    }

    "handle large financial transactions list correctly" in {
      val financialTransactions = (1 to 1000).map { i =>
        FinancialTransaction(
          chargeType = Some(s"CHARGE_$i"),
          mainType = Some(s"MAIN_TYPE_$i"),
          taxPeriodFrom = Some(LocalDate.of(2023, 1, 1)),
          taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
          originalAmount = Some(BigDecimal(s"$i.50")),
          outstandingAmount = Some(BigDecimal(s"$i.00")),
          clearedAmount = Some(BigDecimal(s"${i * 0.5}")),
          items = Some(Seq(
            Item(
              amount = Some(BigDecimal(s"$i")),
              clearingReason = Some(s"Reason $i"),
              paymentReference = Some(s"REF$i"),
              paymentAmount = Some(BigDecimal(s"$i")),
              paymentMethod = Some("Credit Card")
            )
          ))
        )
      }

      val financialData = FinancialData(
        idType = Some("VAT"),
        idNumber = Some("123456789"),
        regimeType = Some("REGIME_LARGE"),
        processingDate = ZonedDateTime.parse("2024-12-12T12:00:00+00:00"),
        financialTransactions = Some(financialTransactions)
      )

      val json = Json.toJson(financialData)
      val deserialized = json.as[FinancialData]

      deserialized mustBe financialData
    }
  }
}
