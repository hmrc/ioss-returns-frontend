/*
 * Copyright 2023 HM Revenue & Customs
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
import generators.Generators
import models.etmp.EtmpObligations
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class VatReturnConnectorSpec extends SpecBase
  with WireMockHelper
  with ScalaCheckPropertyChecks
  with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val etmpObligations: EtmpObligations = arbitraryObligations.arbitrary.sample.value

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.ioss-returns.port" -> server.port)
      .build()
  }

  "VatReturnConnector" - {

    ".getObligations" - {

      val getObligationsUrl: String = s"/ioss-returns/obligations/$iossNumber"

      "must return OK with a payload of ETMP Obligations" in {

        val app = application

        running(app) {
          val connector = app.injector.instanceOf[VatReturnConnector]

          val responseBody = Json.toJson(etmpObligations).toString()

          server.stubFor(
            get(urlEqualTo(getObligationsUrl))
              .willReturn(ok()
                .withBody(responseBody)
              )
          )

          val result = connector.getObligations(iossNumber).futureValue

          result mustBe etmpObligations
        }
      }
    }
  }
}
