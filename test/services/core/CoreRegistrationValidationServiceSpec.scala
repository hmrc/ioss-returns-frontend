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

package services.core

import base.SpecBase
import connectors.core.ValidateCoreRegistrationConnector
import models.UnexpectedResponseStatus
import models.core.{CoreRegistrationValidationResult, Match, TraderId}
import models.etmp.SchemeType
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class CoreRegistrationValidationServiceSpec extends SpecBase with MockitoSugar with ScalaFutures with Matchers with BeforeAndAfterEach {


  private val genericMatch = Match(
    TraderId("333333333"),
    None,
    "EE",
    Some(2),
    None,
    None,
    None,
    None
  )

  private val coreValidationResponses: CoreRegistrationValidationResult =
    CoreRegistrationValidationResult(
      "333333333",
      None,
      "EE",
      traderFound = true,
      Seq(
        genericMatch
      ))

  private val connector = mock[ValidateCoreRegistrationConnector]
  implicit val hc: HeaderCarrier = HeaderCarrier()

  "coreRegistrationValidationService.searchScheme" - {

    "call searchScheme with correct ioss number and must return match data" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: SchemeType = SchemeType.IOSSWithoutIntermediary

      when(connector.validateCoreRegistration(any())(any())) thenReturn Right(coreValidationResponses).toFuture

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector)

      val value = coreRegistrationValidationService.searchIossScheme(iossNumber, previousScheme, None, countryCode).futureValue.get

      value equals genericMatch
    }

    "call searchScheme with correct ioss number with intermediary and must return match data" in {

      val iossNumber: String = "IM333222111"
      val intermediaryNumber: String = "IN555444222"
      val countryCode: String = "DE"
      val previousScheme: SchemeType = SchemeType.IOSSWithoutIntermediary

      when(connector.validateCoreRegistration(any())(any())) thenReturn Right(coreValidationResponses).toFuture

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector)

      val value = coreRegistrationValidationService.searchIossScheme(iossNumber, previousScheme, Some(intermediaryNumber), countryCode).futureValue.get

      value equals genericMatch
    }

    "must return None when no match found" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: SchemeType = SchemeType.IOSSWithIntermediary

      val expectedResponse = coreValidationResponses.copy(matches = Seq[Match]())
      when(connector.validateCoreRegistration(any())(any())) thenReturn Right(expectedResponse).toFuture

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector)

      val value = coreRegistrationValidationService.searchIossScheme(iossNumber, previousScheme, None, countryCode).futureValue

      value mustBe None
    }

    "must return exception when server responds with an error" in {

      val iossNumber: String = "333333333"
      val countryCode: String = "DE"
      val previousScheme: SchemeType = SchemeType.IOSSWithoutIntermediary

      val errorCode = Gen.oneOf(BAD_REQUEST, NOT_FOUND, INTERNAL_SERVER_ERROR).sample.value

      when(connector.validateCoreRegistration(any())(any())) thenReturn Left(UnexpectedResponseStatus(errorCode, "error")).toFuture

      val coreRegistrationValidationService = new CoreRegistrationValidationService(connector)

      val response = intercept[Exception](coreRegistrationValidationService.searchIossScheme(iossNumber, previousScheme, None, countryCode).futureValue)

      response.getMessage must include("Error while validating core registration")
    }
  }
}
