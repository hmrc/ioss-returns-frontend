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

package connectors.core

import base.SpecBase
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, ok, post, urlEqualTo}
import connectors.WireMockHelper
import models.{EisError, UnexpectedResponseStatus}
import models.core._
import org.scalacheck.Gen
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.time.{Seconds, Span}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate}

class ValidateCoreRegistrationConnectorSpec extends SpecBase with WireMockHelper {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private val coreRegistrationRequest = CoreRegistrationRequest(SourceType.VATNumber.toString, None, vrn.vrn, None, "GB")

  private val timestamp = Instant.now

  val getValidateCoreRegistrationUrl = s"/ioss-registration/validate-core-registration"

  private def application: Application =
    applicationBuilder()
      .configure("microservice.services.core-validation.port" -> server.port)
      .build()

  private val validCoreRegistrationResponse: CoreRegistrationValidationResult =
    CoreRegistrationValidationResult(
      searchId = "12345678",
      searchIdIntermediary = Some("12345678"),
      searchIdIssuedBy = "FR",
      traderFound = true,
      matches = Seq(
        Match(
          matchType = MatchType.TransferringMSID,
          traderId = "444444444",
          intermediary = Some("IN4819283759"),
          memberState = "DE",
          exclusionStatusCode = Some(3),
          exclusionDecisionDate = Some(LocalDate.now().format(Match.dateFormatter)),
          exclusionEffectiveDate = Some(LocalDate.now().format(Match.dateFormatter)),
          nonCompliantReturns = Some(0),
          nonCompliantPayments = Some(0)
        )
      )
    )

  "validateCoreRegistration" - {

    "must return Right(CoreRegistrationValidationResult) when the server returns OK for a recognised payload" in {

      val validateCoreRegistration = validCoreRegistrationResponse

      val responseJson = Json.prettyPrint(Json.toJson(validateCoreRegistration))

      server.stubFor(
        post(urlEqualTo(s"$getValidateCoreRegistrationUrl"))
          .willReturn(ok(responseJson))
      )

      running(application) {
        val connector = application.injector.instanceOf[ValidateCoreRegistrationConnector]

        val result = connector.validateCoreRegistration(coreRegistrationRequest).futureValue

        result mustBe Right(validateCoreRegistration)
      }
    }

    "must return an expected response when the server returns a parsable error" in {

      val status = Gen.oneOf(
        BAD_REQUEST,
        NOT_FOUND,
        METHOD_NOT_ALLOWED,
        NOT_ACCEPTABLE,
        UNSUPPORTED_MEDIA_TYPE,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).sample.value

      val errorResponseJson =
        s"""{
           |  "timestamp": "$timestamp",
           |  "transactionId": "None",
           |  "error": "$status",
           |  "errorMessage": "Error"
           |}""".stripMargin


      server.stubFor(
        post(urlEqualTo(s"$getValidateCoreRegistrationUrl"))
          .willReturn(aResponse()
            .withStatus(status)
            .withBody(errorResponseJson)
          )
      )

      running(application) {

        val connector = application.injector.instanceOf[ValidateCoreRegistrationConnector]

        val result = connector.validateCoreRegistration(coreRegistrationRequest).futureValue

        val expectedResponse = EisError(EisErrorResponse(timestamp, s"$status", "Error"))

        result mustBe Left(expectedResponse)

      }
    }

    "must return an expected response when the server returns with an empty body" in {

      val status = Gen.oneOf(
        BAD_REQUEST,
        NOT_FOUND,
        METHOD_NOT_ALLOWED,
        NOT_ACCEPTABLE,
        UNSUPPORTED_MEDIA_TYPE,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).sample.value

      server.stubFor(
        post(urlEqualTo(s"$getValidateCoreRegistrationUrl"))
          .willReturn(aResponse()
            .withStatus(status))
      )

      running(application) {

        val connector = application.injector.instanceOf[ValidateCoreRegistrationConnector]

        val result = connector.validateCoreRegistration(coreRegistrationRequest).futureValue

        val errorResponse = result.swap.getOrElse(throw new RuntimeException("Expected Left, but found Right")).asInstanceOf[EisError].eisErrorResponse

        val expectedResponse = EisError(
         EisErrorResponse(
            errorResponse.timestamp,
            s"$status",
            "The response body was empty"
          )
        )

        result mustBe Left(expectedResponse)
      }
    }

    "must return Left(UnexpectedStatus) when the server returns another error code" in {

      val status = Gen.oneOf(
        BAD_REQUEST,
        NOT_FOUND,
        METHOD_NOT_ALLOWED,
        NOT_ACCEPTABLE,
        UNSUPPORTED_MEDIA_TYPE,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).sample.value

      server.stubFor(
        post(urlEqualTo(s"$getValidateCoreRegistrationUrl"))
          .willReturn(aResponse()
            .withStatus(status)
            .withBody("{}"))
      )

      running(application) {
        val connector = application.injector.instanceOf[ValidateCoreRegistrationConnector]

        val result = connector.validateCoreRegistration(coreRegistrationRequest).futureValue

        result mustBe Left(
          UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
      }
    }

    "must return an Eis Error when the server returns an Http Exception" in {

      val timeout = 30

      val status = Gen.oneOf(
        BAD_REQUEST,
        NOT_FOUND,
        METHOD_NOT_ALLOWED,
        NOT_ACCEPTABLE,
        UNSUPPORTED_MEDIA_TYPE,
        INTERNAL_SERVER_ERROR,
        BAD_GATEWAY,
        SERVICE_UNAVAILABLE
      ).sample.value

      server.stubFor(
        post(urlEqualTo(s"$getValidateCoreRegistrationUrl"))
          .willReturn(aResponse()
            .withStatus(status))
      )

      running(application) {

        val connector = application.injector.instanceOf[ValidateCoreRegistrationConnector]

        whenReady(connector.validateCoreRegistration(coreRegistrationRequest), Timeout(Span(timeout, Seconds))) {
          exp =>
            exp.isLeft mustBe true
            val eisError = exp.swap.getOrElse(throw new RuntimeException("Expected Left, but found Right"))
            eisError mustBe a[EisError]
        }
      }
    }

  }


}
