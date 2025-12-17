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

package controllers.intermediary

import base.SpecBase
import models.saveForLater.SavedUserAnswers
import models.{ContinueReturn, IntermediarySelectedIossNumber, Period, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.ContinueReturnPage
import play.api.inject.bind
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.{IntermediarySelectedIossNumberRepository, SessionRepository}
import services.saveForLater.SaveForLaterService
import utils.FutureSyntax.FutureOps

import java.time.Instant

class StartIntermediarySaveForLaterReturnControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockSaveForLaterService: SaveForLaterService = mock[SaveForLaterService]
  private val mockIntermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository = mock[IntermediarySelectedIossNumberRepository]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  private val iossNumber: String = arbitrary[String].sample.value
  private val period: Period = arbitraryPeriod.arbitrary.sample.value

  private val savedUserAnswers: Seq[SavedUserAnswers] = Gen.listOfN(3, arbitrarySavedUserAnswers.arbitrary).sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockSaveForLaterService,
      mockIntermediarySelectedIossNumberRepository,
      mockSessionRepository
    )
  }

  "StartIntermediarySaveForLaterReturn Controller" - {

    "must redirect to the retrieved saved answers continue url for the given ioss number when the given period exists" in {

      val iossNumber: String = savedUserAnswers.head.iossNumber
      val period: Period = savedUserAnswers.head.period
      val data: JsObject = savedUserAnswers.head.data
      val lastUpdated: Instant = savedUserAnswers.head.lastUpdated

      val intermediarySelectedIossNumber = IntermediarySelectedIossNumber(userAnswersId, intermediaryNumber, iossNumber)

      when(mockSaveForLaterService.getSavedReturnsForClient(any())(any())) thenReturn savedUserAnswers.toFuture
      when(mockIntermediarySelectedIossNumberRepository.set(any())) thenReturn intermediarySelectedIossNumber.toFuture
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val userAnswers: UserAnswers = UserAnswers(
        id = userAnswersId,
        iossNumber = iossNumber,
        period = period,
        data = data,
        lastUpdated = lastUpdated
      )

      val application = applicationBuilder(
        userAnswers = Some(userAnswers),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      )
        .overrides(
          bind[SaveForLaterService].toInstance(mockSaveForLaterService),
          bind[IntermediarySelectedIossNumberRepository].toInstance(mockIntermediarySelectedIossNumberRepository),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartIntermediarySaveForLaterReturnController.onPageLoad(waypoints, iossNumber, period).url)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value mustBe ContinueReturnPage.navigate(userAnswers, ContinueReturn.Continue).url
        verify(mockSaveForLaterService, times(1)).getSavedReturnsForClient(any())(any())
        verify(mockIntermediarySelectedIossNumberRepository, times(1)).set(any())
        verify(mockSessionRepository, times(1)).set(eqTo(userAnswers))
      }
    }

    "must redirect to Start Return As Intermediary Controller for a GET when the given period does not exist" in {

      when(mockSaveForLaterService.getSavedReturnsForClient(any())(any())) thenReturn savedUserAnswers.toFuture

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      )
        .overrides(
          bind[SaveForLaterService].toInstance(mockSaveForLaterService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartIntermediarySaveForLaterReturnController.onPageLoad(waypoints, iossNumber, period).url)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.StartReturnAsIntermediaryController.startReturnAsIntermediary(waypoints, iossNumber).url
        verify(mockSaveForLaterService, times(1)).getSavedReturnsForClient(any())(any())
        verifyNoInteractions(mockIntermediarySelectedIossNumberRepository)
        verifyNoInteractions(mockSessionRepository)
      }
    }
  }
}
