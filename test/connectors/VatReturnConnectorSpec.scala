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
import connectors.ExternalEntryUrlHttpParser.ExternalEntryUrlResponse
import generators.Generators
import models.core.CoreVatReturn
import models.etmp.{EtmpObligations, EtmpVatReturn}
import models.{Country, InvalidJson, UnexpectedResponseStatus}
import models.corrections.ReturnCorrectionValue
import models.external.ExternalEntryUrl
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

class VatReturnConnectorSpec extends SpecBase
  with WireMockHelper
  with ScalaCheckPropertyChecks
  with Generators {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val etmpObligations: EtmpObligations = arbitraryObligations.arbitrary.sample.value
  private val etmpVatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value
  private val returnCorrectionValue: ReturnCorrectionValue = arbitraryReturnCorrectionValue.arbitrary.sample.value

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

    ".get" - {

      val getReturnUrl: String = s"/ioss-returns/return/$period"

      "must return OK with a payload of ETMP VAT Return" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson(etmpVatReturn).toString()

          server.stubFor(
            get(urlEqualTo(getReturnUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.get(period).futureValue

          result mustBe Right(etmpVatReturn)
        }
      }

      "must return Left(InvalidJson) response when invalid json is returned" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson("").toString()

          server.stubFor(
            get(urlEqualTo(getReturnUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.get(period).futureValue

          result mustBe Left(InvalidJson)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]

          server.stubFor(
            get(urlEqualTo(getReturnUrl))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("")
              )
          )

          val result = connector.get(period).futureValue

          result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, ""))
        }
      }
    }

    ".getReturnCorrectionValue" - {

      val country = arbitrary[Country].sample.value

      val getReturnCorrectionValueUrl: String = s"/ioss-returns/max-correction-value/$iossNumber/${country.code}/$period"

      "must return OK with a payload of ETMP VAT Return" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson(returnCorrectionValue).toString()

          server.stubFor(
            get(urlEqualTo(getReturnCorrectionValueUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.getReturnCorrectionValue(iossNumber, country.code, period).futureValue

          result mustBe returnCorrectionValue
        }
      }
    }

    ".submit" - {

      val submitUrl: String = s"/ioss-returns/return/$iossNumber"
      val coreVatReturn: CoreVatReturn = arbitraryCoreVatReturn.arbitrary.sample.value

      "must return OK when the submission is successful" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = "{}"

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(coreVatReturn).toString()))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.submit(coreVatReturn, iossNumber).futureValue

          result.status mustBe OK
        }

      }

      "must return InternalServerError when the submission fails" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(coreVatReturn).toString()))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error")
              )
          )

          val result = connector.submit(coreVatReturn, iossNumber).futureValue

          result.status mustBe INTERNAL_SERVER_ERROR
        }

      }

      "must return OK when the intermediary submission is successful" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = "{}"

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(coreVatReturn).toString()))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.submit(coreVatReturn, iossNumber).futureValue

          result.status mustBe OK
        }

      }

      "must return InternalServerError when the intermediary submission fails" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .withRequestBody(equalToJson(Json.toJson(coreVatReturn).toString()))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error")
              )
          )

          val result = connector.submit(coreVatReturn, iossNumber).futureValue

          result.status mustBe INTERNAL_SERVER_ERROR
        }

      }

    }

    ".getForIossNumber" - {

      val getForIossNumberUrl: String = s"/ioss-returns/return/$period/$iossNumber"

      "must return OK with a payload of ETMP VAT Return" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson(etmpVatReturn).toString()

          server.stubFor(
            get(urlEqualTo(getForIossNumberUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.getForIossNumber(period, iossNumber).futureValue

          result mustBe Right(etmpVatReturn)
        }
      }

      "must return Left(InvalidJson) when invalid JSON is returned" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson("").toString()

          server.stubFor(
            get(urlEqualTo(getForIossNumberUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.getForIossNumber(period, iossNumber).futureValue

          result mustBe Left(InvalidJson)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]

          server.stubFor(
            get(urlEqualTo(getForIossNumberUrl))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error")
              )
          )

          val result = connector.getForIossNumber(period, iossNumber).futureValue

          result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Error"))
        }
      }
    }

    ".getSavedExternalEntry" - {

      val getSavedExternalEntryUrl: String = "/ioss-returns/external-entry"

      val validExternalEntryUrl = ExternalEntryUrl(Some("https://example.com/entry"))
      val externalEntryResponse: ExternalEntryUrlResponse = Right(validExternalEntryUrl)

      "must return OK with a payload of External Entry URL Response" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson(validExternalEntryUrl).toString()

          server.stubFor(
            get(urlEqualTo(getSavedExternalEntryUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.getSavedExternalEntry().futureValue

          result mustBe externalEntryResponse
        }

      }

      "must return Left(InvalidJson) when invalid JSON is returned" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]
          val responseBody = Json.toJson("").toString()

          server.stubFor(
            get(urlEqualTo(getSavedExternalEntryUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseBody)
              )
          )

          val result = connector.getSavedExternalEntry().futureValue

          result mustBe Left(InvalidJson)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server responds with an error code" in {

        running(application) {

          val connector = application.injector.instanceOf[VatReturnConnector]

          server.stubFor(
            get(urlEqualTo(getSavedExternalEntryUrl))
              .willReturn(
                aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("Error")
              )
          )

          val result = connector.getSavedExternalEntry().futureValue

          result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Received unexpected response code 500 with body Error"))
        }
      }
    }
  }
}
