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

package models.external

import base.SpecBase
import models.StandardPeriod
import pages.EmptyWaypoints

import java.time.Month

class ExternalTargetPageSpec extends SpecBase {

  "ExternalTargetPage" - {

    "return the correct URL for YourAccount" in {
      val url = YourAccount.url

      url `mustBe` controllers.routes.YourAccountController.onPageLoad(waypoints).url
    }

    "return the correct URL for ReturnsHistory" in {

      val url = ReturnsHistory.url(iossNumber)

      url `mustBe` controllers.previousReturns.routes.SubmittedReturnsHistoryController.onPageLoad(waypoints, iossNumber).url
    }

    "generate the correct URL for StartReturn with a given period" in {

      val period = StandardPeriod(2023, Month.JANUARY)
      val url = StartReturn.url(iossNumber, period)

      url `mustBe` controllers.routes.StartReturnController.onPageLoad(EmptyWaypoints, iossNumber, period).url
    }

    "generate the correct URL for ContinueReturn with a given period" in {

      val period = StandardPeriod(2023, Month.JANUARY)
      val url = ContinueReturn.url(iossNumber, period)

      url `mustBe` controllers.routes.ContinueReturnController.onPageLoad(iossNumber, period).url
    }

    "return the correct URL for Payment" in {

      val url = Payment.url(iossNumber)

      url `mustBe` controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(waypoints, iossNumber).url
    }
  }
}

