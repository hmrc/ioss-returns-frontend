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

package controllers.submissionResults

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes as baseRoutes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.submissionResults.ReturnSubmissionFailureView

class ReturnSubmissionFailureControllerSpec extends SpecBase {

  "ReturnSubmissionFailure Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val redirectUrl: String = baseRoutes.YourAccountController.onPageLoad(waypoints).url

        val request = FakeRequest(GET, routes.ReturnSubmissionFailureController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReturnSubmissionFailureView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(redirectUrl)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when the user is an Intermediary" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.ReturnSubmissionFailureController.onPageLoad().url)

        val config = application.injector.instanceOf[FrontendAppConfig]

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReturnSubmissionFailureView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(config.intermediaryDashboardUrl)(request, messages(application)).toString
      }
    }
  }
}
