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

package controllers.submissionResults

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.submissionResults.SuccessfullySubmittedView

class SuccessfullySubmittedControllerSpec extends SpecBase {

  "SuccessfullySubmitted Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"

      running(application) {
        val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SuccessfullySubmittedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(returnReference, nilReturn = false, period)(request, messages(application)).toString
      }
    }
  }
}
