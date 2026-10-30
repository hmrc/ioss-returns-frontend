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
import config.FrontendAppConfig
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.Enrolments
import views.html.NoSavedReturnsView

class NoSavedReturnsControllerSpec extends SpecBase {

  private val enrolmentsWithIntermediary = enrolments.copy(
    enrolments = enrolments.enrolments + enrolments.enrolments.head.copy(key = intermediaryEnrolmentKey)
  )

  "NoSavedReturns Controller" - {

    "must return OK and the correct view for a GET when enrolled for IOSS" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoSavedReturnsController.onPageLoad().url)

        val result = route(application, request).value

        val redirect: String = routes.IndexController.onPageLoad.url

        val view = application.injector.instanceOf[NoSavedReturnsView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(redirect)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when enrolled as an Intermediary" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        enrolments = Some(enrolmentsWithIntermediary)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoSavedReturnsController.onPageLoad().url)

        val result = route(application, request).value

        val config = application.injector.instanceOf[FrontendAppConfig]

        val redirect: String = config.intermediaryDashboardUrl

        val view = application.injector.instanceOf[NoSavedReturnsView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(redirect)(request, messages(application)).toString
      }
    }
  }
}
