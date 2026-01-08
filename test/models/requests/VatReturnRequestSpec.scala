/*
 * Copyright 2026 HM Revenue & Customs
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

package models.requests

import base.SpecBase
import models.domain.{VatRate, VatRateType as DomainVatRateType}
import models.{Country, VatOnSales, VatOnSalesChoice}
import play.api.libs.json.{JsError, JsNull, Json}
import queries.{SalesDetails, SalesToCountry}

import java.time.LocalDate

class VatReturnRequestSpec extends SpecBase {

  "VatReturnRequest" - {

    "serialize and deserialize correctly" in {
      val startDate = Some(LocalDate.of(2024, 1, 1))
      val endDate = Some(LocalDate.of(2024, 12, 31))
      val sales = List(
        SalesToCountry(
          Country("AT", "Austria"),
          List(SalesDetails(VatRate(20, DomainVatRateType.Standard), Some(1000), VatOnSales(VatOnSalesChoice.Standard, 200)))
        )
      )
      val vatReturnRequest = VatReturnRequest(Some(vrn), period, startDate, endDate, sales)

      val json = Json.toJson(vatReturnRequest)
      val deserialized = json.as[VatReturnRequest]

      deserialized mustBe vatReturnRequest
    }
    "serialize and deserialize correctly without Vrn" in {
      val startDate = Some(LocalDate.of(2024, 1, 1))
      val endDate = Some(LocalDate.of(2024, 12, 31))
      val sales = List(
        SalesToCountry(
          Country("AT", "Austria"),
          List(SalesDetails(VatRate(20, DomainVatRateType.Standard), Some(1000), VatOnSales(VatOnSalesChoice.Standard, 200)))
        )
      )
      val vatReturnRequest = VatReturnRequest(None, period, startDate, endDate, sales)

      val json = Json.toJson(vatReturnRequest)
      val deserialized = json.as[VatReturnRequest]

      deserialized mustBe vatReturnRequest
    }

    "handle empty sales list correctly" in {
      val startDate = Some(LocalDate.of(2024, 1, 1))
      val endDate = Some(LocalDate.of(2024, 12, 31))

      val vatReturnRequest = VatReturnRequest(Some(vrn), period, startDate, endDate, Nil)

      vatReturnRequest.sales mustBe Nil
    }

    "must handle missing fields during deserialization" in {
      val expectedJson = Json.obj()

      expectedJson.validate[VatReturnRequest] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {
      val expectedJson = Json.obj(
        "endDate" -> 12345,
        "sales" -> Json.obj(
          "country"-> Json.obj(
            "code"-> "AT",
            "name" -> "Austria"),
          "amounts" -> Json.obj(
            "vatRate" -> Json.obj(
              "rate" -> 20,
              "rateType" -> "STANDARD"),
            "vatOnSales" -> Json.obj(
              "choice" -> "option1",
              "amount" -> 200),
            "netValueOfSales" -> 1000)
        ),
        "vrn" -> "123456789",
        "startDate" -> "2024-01-01",
        "period" -> Json.obj(
          "year" -> 2024,
          "month" -> "M3"
        )
      )

      expectedJson.validate[VatReturnRequest] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {
      val expectedJson = Json.obj(
        "endDate" -> JsNull,
        "sales" -> JsNull,
        "vrn" -> JsNull,
        "startDate" -> JsNull,
        "period" -> JsNull
      )

      expectedJson.validate[VatReturnRequest] mustBe a[JsError]
    }
  }
}

