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
import models.requests.SaveForLaterRequest
import models.responses.{ConflictFound, InvalidJson, NotFound, UnexpectedResponseStatus}
import models.saveForLater.SavedUserAnswers
import org.scalacheck.Gen
import org.scalatest.EitherValues
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.{JsArray, JsBoolean, JsObject, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant

class SaveForLaterConnectorSpec extends SpecBase
  with WireMockHelper
  with EitherValues {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()

  private def application: Application = {
    applicationBuilder()
      .configure("microservice.services.ioss-returns.port" -> server.port)
      .build()
  }

  private val savedUserAnswers: Seq[SavedUserAnswers] = Gen.listOfN(3, arbitrarySavedUserAnswers.arbitrary).sample.value

  "SaveForLaterConnector" - {

    ".submit" - {

      val submitUrl: String = "/ioss-returns/save-for-later"

      "must return Right(Some(SavedUserAnswers)) when the server responds with CREATED" in {

        running(application) {

          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))
          val expectedSavedUserAnswers =
            SavedUserAnswers(
              iossNumber, period, data = JsObject(Seq("test" -> Json.toJson("test"))),
              Instant.now(stubClockAtArbitraryDate)
            )
          val responseJson = Json.toJson(expectedSavedUserAnswers)
          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .willReturn(aResponse().withStatus(CREATED).withBody(responseJson.toString()))
          )

          val result = connector.submit(saveForLaterRequest).futureValue

          result mustBe Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(ConflictFound) when the server response with CONFLICT" in {

        running(application) {
          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(post(urlEqualTo(submitUrl)).willReturn(aResponse().withStatus(CONFLICT)))

          val result = connector.submit(saveForLaterRequest).futureValue

          result mustBe Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

        running(application) {
          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(post(urlEqualTo(submitUrl)).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR)))

          val result = connector.submit(saveForLaterRequest).futureValue

          result mustBe Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Unexpected response, status 500 returned"))
        }
      }
    }

    ".get" - {

      val responseJson = Json.toJson(savedUserAnswers.head)
      val getUrl: String = s"/ioss-returns/save-for-later"

      "must return Right(SavedUserAnswers) when the server responds with OK" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(responseJson.toString())
              ))

          connector.get().futureValue mustBe Right(Some(savedUserAnswers.head))
        }
      }

      "must return Right(None) when the server responds with NOT_FOUND" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(
                aResponse().withStatus(NOT_FOUND)
              ))

          connector.get().futureValue mustBe Right(None)
        }
      }

      "must return Left(InvalidJson) when the response body is not a valid Saved Answers Json" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(Json.toJson("test").toString())
              ))

          connector.get().futureValue mustBe Left(InvalidJson)
        }
      }

      "must return Left(ConflictFound) when the server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(
                aResponse().withStatus(CONFLICT)
              ))

          connector.get().futureValue mustBe Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server responds with error" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(
                aResponse().withStatus(123)
              ))

          connector.get().futureValue mustBe Left(UnexpectedResponseStatus(123, s"Unexpected response, status 123 returned"))
        }

      }
    }

    ".submitForIntermediary" - {

      val submitUrl: String = "/ioss-returns/intermediary-save-for-later"

      "must return Right(SavedUserAnswers) when the server responds with CREATED" in {

        running(application) {

          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))
          val expectedSavedUserAnswers =
            SavedUserAnswers(
              iossNumber, period, data = JsObject(Seq("test" -> Json.toJson("test"))),
              Instant.now(stubClockAtArbitraryDate)
            )

          val responseJson = Json.toJson(expectedSavedUserAnswers)
          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .willReturn(aResponse()
                .withStatus(CREATED)
                .withBody(responseJson.toString()))
          )

          val result = connector.submitForIntermediary(saveForLaterRequest).futureValue

          result `mustBe` Right(Some(expectedSavedUserAnswers))
        }
      }

      "must return Left(ConflictFound) when the server response with CONFLICT" in {

        running(application) {

          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .willReturn(aResponse()
                .withStatus(CONFLICT)
              )
          )

          val result = connector.submitForIntermediary(saveForLaterRequest).futureValue

          result `mustBe` Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server response with an error code" in {

        running(application) {

          val saveForLaterRequest = SaveForLaterRequest(iossNumber, period, Json.toJson("test"))

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            post(urlEqualTo(submitUrl))
              .willReturn(aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
              )
          )

          val result = connector.submitForIntermediary(saveForLaterRequest).futureValue

          result `mustBe` Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "Unexpected response, status 500 returned"))
        }
      }
    }

    ".getForIntermediary" - {

      val getUrl: String = "/ioss-returns/intermediary-save-for-later"
      val responseJson = Json.toJson(savedUserAnswers)

      "must return Right(Seq(SavedUserAnswers)) when the server responds with OK" in {

        server.stubFor(
          get(urlEqualTo(getUrl))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(responseJson.toString)
            )
        )

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        val result = connector.getForIntermediary().futureValue

        result `mustBe` Right(savedUserAnswers)
      }

      "must return Right(Seq.empty) when the server responds with OK but there are no SavedUserAnswers" in {

        server.stubFor(
          get(urlEqualTo(getUrl))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(Json.arr().toString)
            )
        )

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        val result = connector.getForIntermediary().futureValue

        result `mustBe` Right(Seq.empty)
      }

      "must return Left(InvalidJson) when the payload cannot be parsed correctly" in {

        val invalidJson: JsArray = Json.arr(Json.obj("invalid" -> "answers"))

        server.stubFor(
          get(urlEqualTo(getUrl))
            .willReturn(aResponse()
              .withStatus(OK)
              .withBody(invalidJson.toString)
            )
        )

        val connector = application.injector.instanceOf[SaveForLaterConnector]

        val result = connector.getForIntermediary().futureValue

        result `mustBe` Left(InvalidJson)
      }

      Seq(NOT_FOUND, CONFLICT, INTERNAL_SERVER_ERROR, 123).foreach { status =>
        s"must return Left(UnexpectedResponseStatus($status)) when the server returns an error" in {

          val errorMessage: String = s"Unexpected response from Intermediary saved User Answers with status $status."

          server.stubFor(
            get(urlEqualTo(getUrl))
              .willReturn(aResponse()
                .withStatus(status)
              )
          )

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          val result = connector.getForIntermediary().futureValue

          result `mustBe` Left(UnexpectedResponseStatus(status, errorMessage))
        }
      }
    }

    ".delete" - {

      val deleteUrl: String = s"/ioss-returns/save-for-later/delete/${period.toString}"

      "must return Right(true) when the server responds with OK" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(deleteUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(JsBoolean(true).toString())
              ))

          connector.delete(period).futureValue mustBe Right(true)
        }
      }

      "must return Left(InvalidJson) when the server responds with an invalid json body" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(deleteUrl))
              .willReturn(
                aResponse().withStatus(OK).withBody(Json.toJson("test").toString())
              ))

          connector.delete(period).futureValue mustBe Left(InvalidJson)
        }
      }

      "must return Left(NotFound) when the server responds with NOT_FOUND" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(deleteUrl))
              .willReturn(
                aResponse().withStatus(NOT_FOUND)
              ))

          connector.delete(period).futureValue mustBe Left(NotFound)
        }
      }

      "must return Left(ConflictFound) when the server responds with CONFLICT" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(deleteUrl))
              .willReturn(
                aResponse().withStatus(CONFLICT)
              ))

          connector.delete(period).futureValue mustBe Left(ConflictFound)
        }
      }

      "must return Left(UnexpectedResponseStatus) when the server responds with error" in {

        running(application) {

          val connector = application.injector.instanceOf[SaveForLaterConnector]

          server.stubFor(
            get(urlEqualTo(deleteUrl))
              .willReturn(
                aResponse().withStatus(123)
              ))

          connector.delete(period).futureValue mustBe Left(UnexpectedResponseStatus(123, s"Unexpected response, status 123 returned"))
        }
      }
    }
  }
}
