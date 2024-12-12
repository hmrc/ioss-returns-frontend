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
import com.github.tomakehurst.wiremock.client.WireMock.*
import generators.Generators
import models.payments.{PaymentPeriod, PaymentRequest, PaymentResponse}
import models.{InvalidJson, UnexpectedResponseStatus}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier


class PaymentConnectorSpec extends SpecBase
  with WireMockHelper
  with ScalaCheckPropertyChecks
  with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure(
        "microservice.services.pay-api.host" -> "localhost",
        "microservice.services.pay-api.port" -> server.port.toString
      )
      .build()
  }

  "PaymentConnector" - {

    ".submit" - {

      val submitUrl: String = "/pay-api/vat-ioss/ni-eu-vat-ioss/journey/start"
      val paymentRequest: PaymentRequest = PaymentRequest(
        ioss = iossNumber,
        period = PaymentPeriod(period.year, period.month, period.paymentDeadline),
        amountInPence = 1000,
        dueDate = Some(period.paymentDeadline)
      )


      "must return a PaymentResponse when the submission is successful (CREATED)" in {

        running(application) {

          val connector = application.injector.instanceOf[PaymentConnector]
          val responseBody = Json.toJson(PaymentResponse("TEST_REFERENCE", "SUCCESS")).toString()

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(paymentRequest).toString()))
              .willReturn(
                aResponse().withStatus(CREATED).withBody(responseBody)
              )
          )

          val result = connector.submit(paymentRequest).futureValue

          result mustBe Right(PaymentResponse("TEST_REFERENCE", "SUCCESS"))
        }
      }

      "must return Left(InvalidJson) when the API returns invalid JSON" in {

        running(application) {

          val connector = application.injector.instanceOf[PaymentConnector]
          val responseBody = Json.toJson("").toString()

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(paymentRequest).toString()))
              .willReturn(
                aResponse().withStatus(CREATED).withBody(responseBody)
              )
          )

          val result = connector.submit(paymentRequest).futureValue

          result mustBe Left(InvalidJson)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the API responds with an error status" in {

        running(application) {

          val connector = application.injector.instanceOf[PaymentConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(paymentRequest).toString()))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error")
              )
          )

          val result = connector.submit(paymentRequest).futureValue

          result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Unexpected response, status 500 returned"))
        }
      }
      
    }
  }
}
