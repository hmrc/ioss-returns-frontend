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

package controllers

import base.SpecBase
import connectors.ReturnStatusConnector
import models.SubmissionStatus
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FutureSyntax.FutureOps
import viewmodels.yourAccount.{CurrentReturns, Return}

class StartOutstandingReturnControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockReturnStatusConnector = mock[ReturnStatusConnector]

  override protected def beforeEach(): Unit = {
    reset(mockReturnStatusConnector)
  }

  "StartOutstandingReturn Controller" - {

    "must redirect to the first due return" in {

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq(Return(
            period = period,
            firstDay = period.firstDay,
            lastDay = period.lastDay,
            dueDate = period.paymentDeadline,
            submissionStatus = SubmissionStatus.Due,
            inProgress = false,
            isOldest = true
          )),
          excluded = false,
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartOutstandingReturnController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.StartReturnController.onPageLoad(EmptyWaypoints, period).url)
      }
    }

    "must redirect to no outstanding returns when none are due return" in {

      when(mockReturnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Right(CurrentReturns(
          Seq.empty,
          excluded = false,
          finalReturnsCompleted = false
        )).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartOutstandingReturnController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(routes.NoReturnsDueController.onPageLoad().url)
      }
    }
  }
}
