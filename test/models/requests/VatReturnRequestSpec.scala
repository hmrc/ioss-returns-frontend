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

package models.requests

import base.SpecBase
import models.domain.{VatRate, VatRateType => DomainVatRateType}
import models.{Country, VatOnSales, VatOnSalesChoice}
import play.api.libs.json.Json
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
      val vatReturnRequest = VatReturnRequest(vrn, period, startDate, endDate, sales)

      val json = Json.toJson(vatReturnRequest)
      val deserialized = json.as[VatReturnRequest]

      deserialized mustBe vatReturnRequest
    }

    "handle empty sales list correctly" in {
      val startDate = Some(LocalDate.of(2024, 1, 1))
      val endDate = Some(LocalDate.of(2024, 12, 31))

      val vatReturnRequest = VatReturnRequest(vrn, period, startDate, endDate, Nil)

      vatReturnRequest.sales mustBe Nil
    }
  }
}

