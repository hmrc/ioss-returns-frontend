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
import models.InvalidJson
import models.payments.PrepareData
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse

class CurrentPaymentsHttpParserSpec extends SpecBase {

  "CurrentPaymentsHttpParser" - {
    "parse a successful response correctly" in {
      val mockResponse = mock[HttpResponse]
      when(mockResponse.status).thenReturn(CREATED)

      val json = Json.obj(
        "duePayments" -> Json.arr(),
        "overduePayments" -> Json.arr(),
        "excludedPayments" -> Json.arr(),
        "totalAmountOwed" -> 1000.00,
        "totalAmountOverdue" -> 500.00,
        "iossNumber" -> "GB12345678"
      )

      when(mockResponse.json).thenReturn(json)

      val result = CurrentPaymentsHttpParser.CurrentPaymentsReads.read("GET", "http://test.url", mockResponse)

      result mustBe Right(PrepareData(
        duePayments = List(),
        overduePayments = List(),
        excludedPayments = List(),
        totalAmountOwed = 1000.00,
        totalAmountOverdue = 500.00,
        iossNumber = "GB12345678"
      ))
    }


    "handle invalid JSON with an error" in {
      val mockResponse = mock[HttpResponse]
      when(mockResponse.status).thenReturn(CREATED)

      val json = Json.obj(
        "duePayments" -> Json.arr(),
        "overduePayments" -> Json.arr(),
        "totalAmountOwed" -> 1000.00
      )

      when(mockResponse.json).thenReturn(json)

      val result = CurrentPaymentsHttpParser.CurrentPaymentsReads.read("GET", "http://test.url", mockResponse)

      result mustBe Left(InvalidJson)
    }

    "return an unexpected error for non-201 status" in {
      val mockResponse = mock[HttpResponse]

      val statusCodes = Seq(400, 404, 500, 503)

      statusCodes.foreach { statusCode =>
        when(mockResponse.status).thenReturn(statusCode)

        val result = CurrentPaymentsHttpParser.CurrentPaymentsReads.read("GET", "http://test.url", mockResponse)
        val expected = Left(UnexpectedResponseStatus(statusCode, s"Unexpected response, status $statusCode returned"))

        val resultBytes = result.toString.getBytes("UTF-8")
        val expectedBytes = expected.toString.getBytes("UTF-8")

        resultBytes mustEqual expectedBytes
      }
    }
  }
}
