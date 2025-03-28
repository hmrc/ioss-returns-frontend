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

import com.github.tomakehurst.wiremock.client.WireMock._
import models.iv.IdentityVerificationResult._
import models.iv._
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{running, INTERNAL_SERVER_ERROR}
import uk.gov.hmrc.http.HeaderCarrier

class IdentityVerificationConnectorSpec
  extends AnyFreeSpec
    with WireMockHelper
    with ScalaFutures
    with Matchers
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application =
    new GuiceApplicationBuilder()
      .configure("microservice.services.identity-verification.port" -> server.port)
      .build()

  "getJourneyStatus" - {

    val journeyId = "123"

    "when the response contains a state we recognise" - {

      "must return that response" in {

        val app = application

        val responseJson =
          """|{
             |  "progress": {
             |    "result": "Success"
             |  }
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(ok(responseJson))
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result.value mustEqual Success
        }
      }
    }

    "when the response contains a state we don't recognise" - {

      "must return None" in {

        val app = application

        val responseJson =
          """|{
             |  "progress": {
             |    "result": "Unknown response"
             |  }
             |}
             |""".stripMargin

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(ok(responseJson))
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result mustBe empty
        }
      }
    }

    "when the call results in an error response code" - {

      "must return an Unexpected Response" in {

        val app = application

        running(app) {
          val connector = app.injector.instanceOf[IdentityVerificationConnector]

          server.stubFor(
            get(urlEqualTo(s"/identity-verification/journey/$journeyId"))
              .willReturn(serverError())
          )

          val result = connector.getJourneyStatus(journeyId).futureValue

          result.value mustEqual IdentityVerificationUnexpectedResponse(INTERNAL_SERVER_ERROR)
        }
      }
    }
  }
}
