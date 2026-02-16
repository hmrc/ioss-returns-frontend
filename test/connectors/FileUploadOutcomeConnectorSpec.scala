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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class FileUploadOutcomeConnectorSpec extends SpecBase with WireMockHelper with Matchers {

  implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure(
        "upscan.file-upload-outcome" -> s"http://localhost:${server.port}/ioss-returns/file-upload-outcome"
      )
      .build()
  }

  private val fileUploadOutcomeJson =
    """
      |{
      |  "fileName": "test.csv"
      |}
      """.stripMargin

    
  "FileUploadOutcomeConnector.get" - {

    "must return Some(fileName) when backend returns 200" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlPathEqualTo("/ioss-returns/file-upload-outcome/fake-ref"))
            .willReturn(aResponse()
              .withStatus(200)
              .withBody(fileUploadOutcomeJson)
            )
        )

        val result = connector.getFileName("fake-ref").futureValue

        result mustBe Some("test.csv")
      }
    }

    "must return None when backend returns 404" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/ioss-returns/file-upload-outcome/unknown-ref"))
            .willReturn(aResponse()
              .withStatus(404)
            )
        )

        val result = connector.getFileName("unknown-ref").futureValue

        result mustBe None
      }
    }

    "must return None when backend returns an error" in {
      val app = application

      running(app) {
        val connector = app.injector.instanceOf[FileUploadOutcomeConnector]

        server.stubFor(
          get(urlEqualTo("/ioss-returns/file-upload-outcome/fail-ref"))
            .willReturn(aResponse()
              .withStatus(500)
              .withBody("Server error")
            )
        )

        val result = connector.getFileName("fail-ref").futureValue

        result mustBe None
      }
    }
  }
}