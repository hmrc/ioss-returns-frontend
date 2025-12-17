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

package controllers.corrections

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.YourAccountPage
import play.api.inject.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.NoOtherPeriodsAvailableView

import scala.concurrent.Future

class NoOtherPeriodsAvailableControllerSpec extends SpecBase {

  private lazy val NoOtherCorrectionPeriodsAvailableRoute = controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(waypoints).url

  val mockSessionRepository: SessionRepository = mock[SessionRepository]

  "CannotStartReturns Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        val redirectUrl: String = YourAccountPage.route(waypoints).url

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, companyName = "CompanyName",  redirectUrl)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when isIntermediary" in {

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        val redirectUrl: String = config.intermediaryDashboardUrl

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, redirectUrl)(request, messages(application)).toString
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are empty for a POST" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad().url
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are not empty for a POST" in {

      val application = applicationBuilder(userAnswers = Some(completedUserAnswersWithCorrections)).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad().url
      }
    }

    "must throw an Exception when Session Repository returns an Exception" in {

      when(mockSessionRepository.set(any())) thenReturn Future.failed(new Exception("Some exception"))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp `mustBe` a[Exception] }
      }
    }
  }
}
