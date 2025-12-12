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

package controllers.previousReturns

import base.SpecBase
import forms.ReturnRegistrationSelectionFormProvider
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SelectedPreviousRegistrationRepository
import services.PreviousRegistrationService
import testUtils.PreviousRegistrationData.{previousRegistrations, selectedPreviousRegistration}
import utils.FutureSyntax.FutureOps
import viewmodels.previousReturns._
import views.html.previousReturns.ReturnRegistrationSelectionView

import scala.concurrent.Future

class ReturnRegistrationSelectionControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val form: Form[PreviousRegistration] = new ReturnRegistrationSelectionFormProvider()(previousRegistrations)

  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockSelectedPreviousRegistrationRepository: SelectedPreviousRegistrationRepository = mock[SelectedPreviousRegistrationRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(mockSelectedPreviousRegistrationRepository)
  }

  "ReturnRegistrationSelection Controller" - {

    "must return OK and the correct view for a GET" - {

      "when there are multiple previous registrations and the form is empty" in {

        val application = applicationBuilder()
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

        running(application) {
          when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

          val request = FakeRequest(GET, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ReturnRegistrationSelectionView]

          status(result) mustBe OK
          contentAsString(result) mustBe
            view(waypoints, form, previousRegistrations)(request, messages(application)).toString
        }
      }

      "when there are multiple previous registrations and the form is prefilled" in {

        val application = applicationBuilder()
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .overrides(bind[SelectedPreviousRegistrationRepository].toInstance(mockSelectedPreviousRegistrationRepository))
          .build()

        running(application) {
          when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
          when(mockSelectedPreviousRegistrationRepository.get(userAnswersId)) thenReturn Some(selectedPreviousRegistration).toFuture

          val request = FakeRequest(GET, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[ReturnRegistrationSelectionView]

          status(result) mustBe OK
          contentAsString(result) mustBe
            view(waypoints, form.fill(selectedPreviousRegistration.previousRegistration), previousRegistrations)(request, messages(application)).toString
        }
      }
    }

    "must redirect to ViewReturnsMultipleRegController for a GET when there is a single previous registration" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.take(1).toFuture

        val request = FakeRequest(GET, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.previousReturns.routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url
      }
    }

    "must redirect to JourneyRecoveryPage for a GET when there are no previous registrations" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn List.empty.toFuture

        val request = FakeRequest(GET, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to ViewReturnsMultipleRegController for a POST when valid data is submitted" in {

      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[SelectedPreviousRegistrationRepository].toInstance(mockSelectedPreviousRegistrationRepository))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockSelectedPreviousRegistrationRepository.set(any())) thenReturn Future.successful(selectedPreviousRegistration)

        val request = FakeRequest(POST, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)
          .withFormUrlEncodedBody(("value", selectedPreviousRegistration.previousRegistration.iossNumber))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.previousReturns.routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

        val request = FakeRequest(POST, routes.ReturnRegistrationSelectionController.onPageLoad(waypoints).url)
          .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        val view = application.injector.instanceOf[ReturnRegistrationSelectionView]

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustEqual view(waypoints, form.bind(Map("value" -> "")), previousRegistrations)(request, messages(application)).toString
      }
    }
  }
}
