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
import controllers.actions.FakeIntermediaryIdentifierAction
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import views.html.{JourneyRecoveryContinueView, JourneyRecoveryStartAgainView}

class JourneyRecoveryControllerSpec extends SpecBase {

  "JourneyRecovery Controller" - {

    "when a relative continue Url is supplied" - {

      "must return OK and the continue view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("/foo")
          val request     = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val continueView = application.injector.instanceOf[JourneyRecoveryContinueView]

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` continueView(continueUrl.unsafeValue, isIntermediary = false)(request, messages(application)).toString
        }
      }
    }

    "when an absolute continue Url is supplied" - {

      "must return OK and the start again view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val continueUrl = RedirectUrl("https://foo.com")
          val request     = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          val redirectUrl: String = routes.IndexController.onPageLoad.url

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` startAgainView(redirectUrl)(request, messages(application)).toString
        }
      }

      "must return OK and the start again view for an Intermediary" in {

        val fakeIntermediaryIdentifierAction: FakeIntermediaryIdentifierAction = FakeIntermediaryIdentifierAction()

        val application = applicationBuilder(
          userAnswers = None,
          getIdentifierAction = Some(fakeIntermediaryIdentifierAction)
        ).build()

        running(application) {
          val continueUrl = RedirectUrl("https://foo.com")
          val request     = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad(Some(continueUrl)).url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          val config = application.injector.instanceOf[FrontendAppConfig]

          val redirectUrl: String = config.intermediaryDashboardUrl

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` startAgainView(redirectUrl)(request, messages(application)).toString
        }
      }
    }

    "when no continue Url is supplied" - {

      "must return OK and the start again view" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.JourneyRecoveryController.onPageLoad().url)

          val result = route(application, request).value

          val startAgainView = application.injector.instanceOf[JourneyRecoveryStartAgainView]

          val redirectUrl: String = routes.IndexController.onPageLoad.url

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` startAgainView(redirectUrl)(request, messages(application)).toString
        }
      }
    }
  }
}
