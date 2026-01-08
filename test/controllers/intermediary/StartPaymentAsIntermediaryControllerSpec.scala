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

package controllers.intermediary

import base.SpecBase
import models.IntermediarySelectedIossNumber
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.IntermediarySelectedIossNumberRepository
import utils.FutureSyntax.FutureOps

import java.time.Instant

class StartPaymentAsIntermediaryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockIntermediarySelectedIossNumberRepository = mock[IntermediarySelectedIossNumberRepository]

  override protected def beforeEach(): Unit = {
    reset(mockIntermediarySelectedIossNumberRepository)
  }

  "StartPaymentAsIntermediary Controller" - {

    "must redirect to the payment journey" in {

      when(mockIntermediarySelectedIossNumberRepository.set(any())) thenReturn
        IntermediarySelectedIossNumber(userId = userAnswersId, intermediaryNumber = intermediaryNumber, iossNumber = iossNumber, lastUpdated = Instant.now).toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), maybeIntermediaryNumber = Some(intermediaryNumber))
        .overrides(bind[IntermediarySelectedIossNumberRepository].toInstance(mockIntermediarySelectedIossNumberRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.StartPaymentAsIntermediaryController.startPaymentAsIntermediary(iossNumber = iossNumber).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(EmptyWaypoints).url)
      }
    }

  }
}
