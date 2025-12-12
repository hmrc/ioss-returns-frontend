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
import generators.Generators
import models.enrolments.EACDEnrolments
import models.etmp.intermediary.IntermediaryRegistrationWrapper
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class IntermediaryRegistrationConnectorSpec
  extends SpecBase
    with WireMockHelper
    with ScalaCheckPropertyChecks
    with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.ioss-intermediary-registration.port" -> server.port)
      .build()
  }

  ".get" - {
    def url(intermediaryNumber: String) = s"/ioss-intermediary-registration/get-registration/$intermediaryNumber"

    "must return a registration when the server provides one" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[IntermediaryRegistrationConnector]
        val registration = arbitrary[IntermediaryRegistrationWrapper].sample.value

        val responseBody = Json.toJson(registration).toString

        server.stubFor(get(urlEqualTo(url(intermediaryNumber))).willReturn(ok().withBody(responseBody)))

        val result = connector.get(intermediaryNumber).futureValue

        result mustEqual registration
      }
    }

  }

  ".getAccounts" - {
    val url = s"/ioss-intermediary-registration/accounts"

    "must return a registration when the server provides one" in {

      val app = application

      running(app) {
        val connector = app.injector.instanceOf[IntermediaryRegistrationConnector]
        val eACDEnrolments = arbitrary[EACDEnrolments].sample.value

        val responseBody = Json.toJson(eACDEnrolments).toString

        server.stubFor(get(urlEqualTo(url)).willReturn(ok().withBody(responseBody)))

        val result = connector.getAccounts().futureValue

        result mustEqual eACDEnrolments
      }
    }

  }

}
