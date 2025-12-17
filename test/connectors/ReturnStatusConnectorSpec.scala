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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import formats.Format
import models.SubmissionStatus.Due
import models.responses.{InvalidJson, UnexpectedResponseStatus}
import models.{Period, PeriodWithStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.yourAccount.{CurrentReturns, Return}

import java.time.LocalDate

class ReturnStatusConnectorSpec extends SpecBase
  with WireMockHelper
  with EitherValues  {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.ioss-returns.port" -> server.port)
      .build()

  ".getCurrentReturns" - {

    val getCurrentReturnsUrl: String = s"/ioss-returns/vat-returns/current-returns/$iossNumber"

    val period = arbitrary[Period].sample.value
    val responseJson = Json.toJson(
      CurrentReturns(
        Seq(Return.fromPeriod(period, Due, false, false)),
        excluded = false,
        finalReturnsCompleted = false,
        completeOrExcludedReturns = List.empty
      )
    )

    "return a Returns model" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(getCurrentReturnsUrl))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            ))

        connector.getCurrentReturns(iossNumber).futureValue mustBe Right(CurrentReturns(
          Seq(Return.fromPeriod(period, Due, false, false)), false, false, List.empty))
      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]
        val responseBody = Json.toJson("").toString()

        server.stubFor(
          get(urlEqualTo(getCurrentReturnsUrl))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseBody)
            ))

        connector.getCurrentReturns(iossNumber).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(getCurrentReturnsUrl))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val result = connector.getCurrentReturns(iossNumber).futureValue
        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Unexpected response, status 500 returned"))
      }
    }

  }

  ".listStatuses" - {

    val periodWithStatus = arbitrary[PeriodWithStatus].sample.value
    val responseJson = Json.toJson(Seq(periodWithStatus))
    val commencementDate = LocalDate.now()
    val listStatusUrl: String = s"/ioss-returns/vat-returns/statuses/$iossNumber/${Format.dateTimeFormatter.format(commencementDate)}"

    "return a list of statuses for a single period" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(listStatusUrl))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseJson.toString())
            )
        )

        connector.listStatuses(iossNumber, commencementDate).futureValue mustBe Right(Seq(periodWithStatus))

      }
    }

    "must return Left(InvalidJson) when the server responds with an incorrectly formatted JSON payload" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]
        val responseBody = Json.toJson("").toString()

        server.stubFor(
          get(urlEqualTo(listStatusUrl))
            .willReturn(
              aResponse().withStatus(OK).withBody(responseBody)
            )
        )

        connector.listStatuses(iossNumber, commencementDate).futureValue mustBe Left(InvalidJson)
      }
    }

    "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

      running(application) {
        val connector = application.injector.instanceOf[ReturnStatusConnector]

        server.stubFor(
          get(urlEqualTo(listStatusUrl))
            .willReturn(
              aResponse().withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val result = connector.listStatuses(iossNumber, commencementDate).futureValue
        result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Unexpected response, status 500 returned"))
      }
    }
  }
}
