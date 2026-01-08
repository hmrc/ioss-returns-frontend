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

package services.saveForLater

import base.SpecBase
import connectors.SaveForLaterConnector
import models.Period
import models.requests.SaveForLaterRequest
import models.responses.{ErrorResponse, InternalServerError}
import models.saveForLater.SavedUserAnswers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatest.PrivateMethodTester.PrivateMethod
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SaveForLaterServiceSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private implicit val hc: HeaderCarrier = new HeaderCarrier()

  private val mockSaveForLaterConnector: SaveForLaterConnector = mock[SaveForLaterConnector]

  private val savedUserAnswers: Seq[SavedUserAnswers] = Gen.listOfN(3, arbitrarySavedUserAnswers.arbitrary).sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSaveForLaterConnector)
  }

  "SaveForLaterService" - {

    ".submitForIntermediary" - {

      "must return Some(SavedUserAnswers) when the connector returns Right with a successful payload" in {

        when(mockSaveForLaterConnector.submitForIntermediary(any())(any())) thenReturn Right(Some(savedUserAnswers.head)).toFuture

        val saveForLaterRequest = SaveForLaterRequest(
          iossNumber = savedUserAnswers.head.iossNumber,
          period = savedUserAnswers.head.period,
          data = savedUserAnswers.head.data
        )

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.submitForIntermediary(saveForLaterRequest).futureValue

        result `mustBe` Some(savedUserAnswers.head)
        verify(mockSaveForLaterConnector, times(1)).submitForIntermediary(eqTo(saveForLaterRequest))(any())
      }

      "must throw an Exception when the connector returns an error" in {

        val errorResponse: ErrorResponse = InternalServerError
        val errorMessage: String = s"There was an error whilst submitting saved user answers with error: ${errorResponse.body}"

        when(mockSaveForLaterConnector.submitForIntermediary(any())(any())) thenReturn Left(errorResponse).toFuture

        val saveForLaterRequest = SaveForLaterRequest(
          iossNumber = savedUserAnswers.head.iossNumber,
          period = savedUserAnswers.head.period,
          data = savedUserAnswers.head.data
        )

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.submitForIntermediary(saveForLaterRequest).failed

        whenReady(result) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }

        verify(mockSaveForLaterConnector, times(1)).submitForIntermediary(eqTo(saveForLaterRequest))(any())
      }
    }

    ".getAllClientSavedAnswers" - {

      "must return SavedUserAnswers when the connector returns Right(SavedUserAnswers)" in {

        when(mockSaveForLaterConnector.getForIntermediary()(any())) thenReturn Right(savedUserAnswers).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val privateMethodCall = PrivateMethod[Future[Seq[SavedUserAnswers]]](Symbol("getAllClientSavedAnswers"))
        val result = service invokePrivate privateMethodCall(hc)

        result.futureValue `mustBe` savedUserAnswers
        verify(mockSaveForLaterConnector, times(1)).getForIntermediary()(any())
      }

      "must throw an Exception when the connector returns an error" in {

        val error: ErrorResponse = InternalServerError
        val errorMessage: String = s"An error occurred retrieving saved user answers: ${error.body}"

        when(mockSaveForLaterConnector.getForIntermediary()(any())) thenReturn Left(error).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val privateMethodCall = PrivateMethod[Future[Seq[SavedUserAnswers]]](Symbol("getAllClientSavedAnswers"))
        val result = service invokePrivate privateMethodCall(hc)

        whenReady(result.failed) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }
        verify(mockSaveForLaterConnector, times(1)).getForIntermediary()(any())
      }
    }

    ".getSavedReturnsForClient" - {

      val iossNumber: String = savedUserAnswers.head.iossNumber

      "must return SavedUserAnswers for the given IOSS number when the connector returns Right(SavedUserAnswers)" in {

        when(mockSaveForLaterConnector.getForIntermediary()(any())) thenReturn Right(savedUserAnswers).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.getSavedReturnsForClient(iossNumber).futureValue

        result `mustBe` Seq(savedUserAnswers.head)
        verify(mockSaveForLaterConnector, times(1)).getForIntermediary()(any())
      }

      "must throw an Exception when the connector returns an error" in {

        val error: ErrorResponse = InternalServerError
        val errorMessage: String = s"An error occurred retrieving saved user answers: ${error.body}"

        when(mockSaveForLaterConnector.getForIntermediary()(any())) thenReturn Left(error).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.getSavedReturnsForClient(iossNumber).failed

        whenReady(result) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }
        verify(mockSaveForLaterConnector, times(1)).getForIntermediary()(any())
      }
    }

    ".deleteSavedUserAnswers" - {

      val period: Period = savedUserAnswers.head.period

      "must return true when the connector returns Right(true)" in {

        when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Right(true).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.deleteSavedUserAnswers(period).futureValue

        result `mustBe` true
        verify(mockSaveForLaterConnector, times(1)).delete(eqTo(period))(any())
      }

      "must throw an Exception when the connector returns an error" in {

        val error: ErrorResponse = InternalServerError
        val errorMessage: String = s"An error occurred deleting saved user answers for period: $period with error: ${error.body}"

        when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Left(error).toFuture

        val service = new SaveForLaterService(mockSaveForLaterConnector)

        val result = service.deleteSavedUserAnswers(period).failed

        whenReady(result) { exp =>
          exp `mustBe` a[Exception]
          exp.getMessage `mustBe` errorMessage
        }
        verify(mockSaveForLaterConnector, times(1)).delete(eqTo(period))(any())
      }
    }
  }
}
