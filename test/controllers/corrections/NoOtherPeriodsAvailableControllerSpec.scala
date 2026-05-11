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

package controllers.corrections

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.FakeGetRegistrationActionProvider
import controllers.routes
import models.RegistrationWrapper
import models.etmp.VatCustomerInfo
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.YourAccountPage
import play.api.inject.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.intermediary.DashboardNavigationService
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import utils.FutureSyntax.FutureOps
import views.html.NoOtherPeriodsAvailableView

import scala.concurrent.Future

class NoOtherPeriodsAvailableControllerSpec extends SpecBase with BeforeAndAfterEach {

  private lazy val NoOtherCorrectionPeriodsAvailableRoute =
    controllers.corrections.routes.NoOtherCorrectionPeriodsAvailableController.onPageLoad(waypoints, iossNumber).url

  private val mockSessionRepository: SessionRepository = mock[SessionRepository]
  private val mockDashboardNavigationService: DashboardNavigationService = mock[DashboardNavigationService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockDashboardNavigationService)
  }

  "CannotStartReturns Controller" - {

    "must return OK and the correct view for a GET" in {

      val redirectUrl: String = YourAccountPage.route(waypoints).url
      when(mockDashboardNavigationService.getAppropriateDashboardUrl(any(), any(), any())(any())) thenReturn redirectUrl.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[DashboardNavigationService].toInstance(mockDashboardNavigationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, iossNumber, isIntermediary = false, companyName = "CompanyName",  redirectUrl)(request, messages(application)).toString
        verify(mockDashboardNavigationService, times(1)).getAppropriateDashboardUrl(any(), any(), any())(any())
      }
    }

    "must return OK and the correct view for a GET when isIntermediary" in {

      val vatInfo: VatCustomerInfo = registrationWrapper.vatInfo.get.copy(organisationName = Some("CompanyName"))
      val registration: RegistrationWrapper = registrationWrapper.copy(vatInfo = Some(vatInfo))
      val companyName: String = registration.getCompanyName()

      val onlyIntermediaryEnrolment: Enrolments = Enrolments(
        Set(
          Enrolment(
            key = intermediaryEnrolmentKey,
            identifiers = Seq(
              EnrolmentIdentifier("IntNumber", intermediaryNumber)
            ),
            state = "Activated"
          )
        )
      )

      val fakeProvider =
        new FakeGetRegistrationActionProvider(
          registration,
          maybeIntermediaryNumber = Some(intermediaryNumber),
          enrolments = Some(onlyIntermediaryEnrolment)
        )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        getRegistrationAction = Some(fakeProvider),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      ).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoOtherPeriodsAvailableController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoOtherPeriodsAvailableView]

        val config = application.injector.instanceOf[FrontendAppConfig]

        val redirectUrl: String = config.intermediaryDashboardUrl

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(waypoints, iossNumber, isIntermediary = true, companyName, redirectUrl)(request, messages(application)).toString
        verifyNoInteractions(mockDashboardNavigationService)
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are empty for a POST" in {

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[SessionRepository].toInstance(mockSessionRepository)
        ).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad(waypoints, iossNumber).url
      }
    }

    "must redirect to CheckYourAnswersController when completed correction periods are not empty for a POST" in {

      val application = applicationBuilder(userAnswers = Some(completedUserAnswersWithCorrections)).build()

      running(application) {
        val request = FakeRequest(POST, NoOtherCorrectionPeriodsAvailableRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad(waypoints, iossNumber).url
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
