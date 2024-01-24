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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock._
import models.financialdata.{Charge, FinancialData, FinancialTransaction, Item}
import models.payments.{Payment, PaymentStatus, PrepareData}
import models.{InvalidJson, Period, UnexpectedResponseStatus}
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, Month, ZoneOffset, ZonedDateTime}

class FinancialDataConnectorSpec extends SpecBase with WireMockHelper with FinancialDataConnectorFixture {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val baseUrl: String = "ioss-returns/financial-data"
  private val charge: Charge = arbitraryCharge.arbitrary.sample.value

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.ioss-returns.port" -> server.port)
      .build()
  }

  private val now = zonedNow.toLocalDate
  "getFinancialData" - {

    "when the server returns OK and a recognised payload" - {
      "must return a FinancialDataResponse" in {
        val app = application

        server.stubFor(
          get(s"/ioss-returns/financial-data/get/$now")
            .willReturn(ok(responseJsonFinancialData))
        )

        running(app) {
          val connector = app.injector.instanceOf[FinancialDataConnector]
          val result = connector.getFinancialData(now).futureValue

          result mustEqual expectedResultFinancialData
        }
      }
    }
  }

  "prepareData" - {

    "when the server returns OK and a recognised payload" - {
      "must return a FinancialDataResponse" in {
        val app = application

        server.stubFor(
          get(urlEqualTo("/ioss-returns/financial-data/prepare"))
            .willReturn(ok(responseJsonPrepareFinancialData))
        )

        running(app) {
          val connector = app.injector.instanceOf[FinancialDataConnector]
          val result = connector.prepareFinancialData().futureValue

          result mustEqual Right(expectedPrepareFinancialData)
        }
      }
    }
  }

  ".getCharge" - {

    val url = s"/$baseUrl/charge/$period"

    "must return Some(Charge) when successful" in {

      running(application) {

        val connector = application.injector.instanceOf[FinancialDataConnector]
        val responseJson = Json.toJson(charge).toString()

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson)
            )
        )

        val result = connector.getCharge(period).futureValue

        result mustBe Right(Some(charge))
      }
    }

    "must return Right(None) when no charge is retrieved" in {

      running(application) {

        val connector = application.injector.instanceOf[FinancialDataConnector]
        val responseJson = Json.toJson(None).toString()

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson)
            )
        )

        val result = connector.getCharge(period).futureValue

        result mustBe Right(None)
      }
    }

    "must return Left(InvalidJson) response when invalid json is returned" in {

      running(application) {

        val connector = application.injector.instanceOf[FinancialDataConnector]
        val responseJson = Json.toJson("").toString()

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson)
            )
        )

        val result = connector.getCharge(period).futureValue

        result mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {

        val connector = application.injector.instanceOf[FinancialDataConnector]

        server.stubFor(
          get(urlEqualTo(url))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("")
            )
        )

        val result = connector.getCharge(period).futureValue

        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, ""))
      }
    }
  }
}

trait FinancialDataConnectorFixture {
  self: SpecBase =>

  val zonedNow: ZonedDateTime = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

  val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  val dateTo: LocalDate = zonedNow.toLocalDate

  val expectedPrepareFinancialData: PrepareData = PrepareData(
    Nil,
    List(
      Payment(
        Period(2021, Month.SEPTEMBER),
        1000,
        LocalDate.of(2021, 10, 31),
        PaymentStatus.Partial
      )
    ),
    1000,
    1000,
    "IM9001234567"
  )
  val responseJsonPrepareFinancialData: String =
    """{
      |  "duePayments": [
      |  ],
      |  "overduePayments": [
      |    {
      |      "period": {
      |        "year": 2021,
      |        "month": {
      |         "month": 9
      |         }
      |      },
      |      "amountOwed": 1000,
      |      "dateDue": "2021-10-31",
      |      "paymentStatus": "PARTIAL"
      |    }
      |  ],
      |  "totalAmountOwed": 1000,
      |  "totalAmountOverdue": 1000,
      |  "iossNumber": "IM9001234567"
      |
      |}""".stripMargin

  val responseJsonFinancialData: String =
    s"""{
       | "idType": "IOSS",
       | "idNumber": "123456789",
       | "regimeType": "ECOM",
       | "processingDate": "$zonedNow",
       | "financialTransactions": [
       |   {
       |     "chargeType": "G Ret AT EU-OMS",
       |     "taxPeriodFrom": "${dateFrom}",
       |     "taxPeriodTo": "${dateTo}",
       |     "originalAmount": 1000,
       |     "outstandingAmount": 500,
       |     "clearedAmount": 500,
       |     "items": [
       |       {
       |         "amount": 500,
       |         "clearingReason": "",
       |         "paymentReference": "",
       |         "paymentAmount": 500,
       |         "paymentMethod": ""
       |       }
       |     ]
       |   }
       | ]
       |}""".stripMargin

  private val items = Seq(
    Item(
      amount = Some(BigDecimal(500)),
      clearingReason = Some(""),
      paymentReference = Some(""),
      paymentAmount = Some(BigDecimal(500)),
      paymentMethod = Some("")
    )
  )

  val financialTransactions: Seq[FinancialTransaction] = Seq(
    FinancialTransaction(
      chargeType = Some("G Ret AT EU-OMS"),
      mainType = None,
      taxPeriodFrom = Some(dateFrom),
      taxPeriodTo = Some(dateTo),
      originalAmount = Some(BigDecimal(1000)),
      outstandingAmount = Some(BigDecimal(500)),
      clearedAmount = Some(BigDecimal(500)),
      items = Some(items)
    )
  )

  val expectedResultFinancialData: FinancialData = FinancialData(
    idType = Some("IOSS"),
    idNumber = Some("123456789"),
    regimeType = Some("ECOM"),
    processingDate = zonedNow,
    financialTransactions = Option(financialTransactions))
}