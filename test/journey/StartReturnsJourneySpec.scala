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

package journey

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.{NoOtherPeriodsAvailablePage, SoldGoodsPage, StartReturnPage, WantToUploadFilePage, YourAccountPage}

class StartReturnsJourneySpec extends AnyFreeSpec with JourneyHelpers with SpecBase {

  private val mockAppConfig = mock[FrontendAppConfig]
  "when there are no other return periods available" - {

    "user can't complete any other returns other than the available return period" in {
      startingFrom(YourAccountPage, answers = UserAnswers(userAnswersId, iossNumber, period))
        .run(
          goTo(StartReturnPage(period, mockAppConfig)),
          submitAnswer(StartReturnPage(period, mockAppConfig), false),
          pageMustBe(NoOtherPeriodsAvailablePage)
        )
    }

    "must ask user if they made eligible sales when they start the return for the available period" in {
      startingFrom(YourAccountPage, answers = UserAnswers(userAnswersId, iossNumber, period))
        .run(
          goTo(StartReturnPage(period, mockAppConfig)),
          submitAnswer(StartReturnPage(period, mockAppConfig), true),
          pageMustBe(SoldGoodsPage)
        )
    }

    "must ask user if they want to upload a file for the return for the available period" in {
      when(mockAppConfig.intermediaryEnabled) thenReturn true
      startingFrom(YourAccountPage, answers = UserAnswers(userAnswersId, iossNumber, period))
        .run(
          goTo(StartReturnPage(period, mockAppConfig)),
          submitAnswer(StartReturnPage(period, mockAppConfig), true),
          pageMustBe(WantToUploadFilePage)
        )
    }
  }
}