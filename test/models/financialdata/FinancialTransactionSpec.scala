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
import play.api.libs.json.{JsNull, Json, JsError}

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

    "handle empty items list correctly" in {
      val financialTransaction = FinancialTransaction(
        chargeType = Some("CHARGE_2"),
        mainType = Some("MAIN_TYPE_2"),
        taxPeriodFrom = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
        originalAmount = Some(BigDecimal("1500.75")),
        outstandingAmount = Some(BigDecimal("300.50")),
        clearedAmount = Some(BigDecimal("1200.25")),
        items = Some(Seq.empty)
      )

      val json = Json.toJson(financialTransaction)
      val deserialized = json.as[FinancialTransaction]

      deserialized mustBe financialTransaction
    }

    "handle partially populated fields correctly" in {
      val financialTransaction = FinancialTransaction(
        chargeType = Some("CHARGE_3"),
        mainType = None,
        taxPeriodFrom = None,
        taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
        originalAmount = Some(BigDecimal("2000.00")),
        outstandingAmount = None,
        clearedAmount = Some(BigDecimal("1500.00")),
        items = None
      )

      val json = Json.toJson(financialTransaction)
      val deserialized = json.as[FinancialTransaction]

      deserialized mustBe financialTransaction
    }

    "handle null fields in JSON correctly" in {
      val json = Json.obj(
        "chargeType" -> JsNull,
        "mainType" -> JsNull,
        "taxPeriodFrom" -> JsNull,
        "taxPeriodTo" -> JsNull,
        "originalAmount" -> JsNull,
        "outstandingAmount" -> JsNull,
        "clearedAmount" -> JsNull,
        "items" -> JsNull
      )

      val expectedFinancialTransaction = FinancialTransaction(
        chargeType = None,
        mainType = None,
        taxPeriodFrom = None,
        taxPeriodTo = None,
        originalAmount = None,
        outstandingAmount = None,
        clearedAmount = None,
        items = None
      )

      json.as[FinancialTransaction] mustBe expectedFinancialTransaction
    }

    "fail to deserialize invalid JSON" in {
      val invalidJson = Json.obj(
        "chargeType" -> "CHARGE_4",
        "mainType" -> "MAIN_TYPE_4",
        "taxPeriodFrom" -> "invalid-date",
        "taxPeriodTo" -> "2023-12-31",
        "originalAmount" -> "not-a-number",
        "outstandingAmount" -> "300.50",
        "clearedAmount" -> "1500.00",
        "items" -> Json.arr(
          Json.obj(
            "amount" -> "not-a-number",
            "clearingReason" -> "Some reason",
            "paymentReference" -> "REF123456",
            "paymentAmount" -> "500",
            "paymentMethod" -> "Credit Card"
          )
        )
      )

      invalidJson.validate[FinancialTransaction] mustBe a[JsError]
    }

    "handle large items list correctly" in {
      val items = (1 to 1000).map { i =>
        Item(
          amount = Some(BigDecimal(s"$i.00")),
          clearingReason = Some(s"Reason_$i"),
          paymentReference = Some(s"REF_$i"),
          paymentAmount = Some(BigDecimal(s"$i.00")),
          paymentMethod = Some("Credit Card")
        )
      }

      val financialTransaction = FinancialTransaction(
        chargeType = Some("CHARGE_5"),
        mainType = Some("MAIN_TYPE_5"),
        taxPeriodFrom = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
        originalAmount = Some(BigDecimal("5000.00")),
        outstandingAmount = Some(BigDecimal("1000.00")),
        clearedAmount = Some(BigDecimal("4000.00")),
        items = Some(items)
      )

      val json = Json.toJson(financialTransaction)
      val deserialized = json.as[FinancialTransaction]

      deserialized mustBe financialTransaction
    }

    "serialize correctly to JSON" in {
      val financialTransaction = FinancialTransaction(
        chargeType = Some("CHARGE_6"),
        mainType = Some("MAIN_TYPE_6"),
        taxPeriodFrom = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodTo = Some(LocalDate.of(2023, 12, 31)),
        originalAmount = Some(BigDecimal("2500.00")),
        outstandingAmount = Some(BigDecimal("500.00")),
        clearedAmount = Some(BigDecimal("2000.00")),
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

      val expectedJson = Json.obj(
        "chargeType" -> "CHARGE_6",
        "mainType" -> "MAIN_TYPE_6",
        "taxPeriodFrom" -> "2023-01-01",
        "taxPeriodTo" -> "2023-12-31",
        "originalAmount" -> 2500.00,
        "outstandingAmount" -> 500.00,
        "clearedAmount" -> 2000.00,
        "items" -> Json.arr(
          Json.obj(
            "amount" -> 500,
            "clearingReason" -> "Clearing Reason",
            "paymentReference" -> "REF123456",
            "paymentAmount" -> 500,
            "paymentMethod" -> "Credit Card"
          )
        )
      )

      Json.toJson(financialTransaction) mustBe expectedJson
    }
  }

}
