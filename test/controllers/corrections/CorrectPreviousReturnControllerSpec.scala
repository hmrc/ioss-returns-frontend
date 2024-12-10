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

package controllers.corrections

import base.SpecBase
import controllers.routes
import forms.corrections.CorrectPreviousReturnFormProvider
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectPreviousReturnPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ObligationsService
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectPreviousReturnView

import scala.concurrent.Future

class CorrectPreviousReturnControllerSpec extends SpecBase with MockitoSugar {

  private val obligationService: ObligationsService = mock[ObligationsService]

  private val singleEtmpObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AL"
    )
  )

  private val etmpObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AL"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AK"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "22AK"
    )
  )

  val formProvider = new CorrectPreviousReturnFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val correctPreviousReturnRoute: String = controllers.corrections.routes.CorrectPreviousReturnController.onPageLoad(waypoints).url

  "CorrectPreviousReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, maybeExclusion = None, isFinalReturn = false)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val userAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage(0), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(true),
          waypoints,
          period,
          maybeExclusion = None,
          isFinalReturn = false
        )(request, messages(application)).toString
      }
    }

    "must redirect to the multiple period page when valid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage(0), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectPreviousReturnPage(3).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must redirect to the single period page when valid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn singleEtmpObligationDetails.toFuture

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectPreviousReturnPage(0), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectPreviousReturnPage(0).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectPreviousReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          waypoints,
          period,
          maybeExclusion = None,
          isFinalReturn = false
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctPreviousReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctPreviousReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
