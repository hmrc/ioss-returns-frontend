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
import forms.corrections.RemovePeriodCorrectionFormProvider
import models.{Country, Index}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, RemovePeriodCorrectionPage, VatAmountCorrectionCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.corrections.RemovePeriodCorrectionView

import scala.concurrent.Future

class RemovePeriodCorrectionControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new RemovePeriodCorrectionFormProvider()
  private val form = formProvider()

  private lazy val removePeriodCorrectionRoute = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(waypoints, index).url

  "RemovePeriodCorrection Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removePeriodCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemovePeriodCorrectionView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(RemovePeriodCorrectionPage(index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removePeriodCorrectionRoute)

        val view = application.injector.instanceOf[RemovePeriodCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), waypoints, period, index)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to CorrectPreviousReturn page when true is submitted with a single correction period" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removePeriodCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemovePeriodCorrectionPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to VatPeriodCorrectionsList page when true is submitted and there are multiple periods" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(10)).success.value
        .set(CorrectionReturnPeriodPage(Index(1)), period).success.value
        .set(CorrectionCountryPage(Index(1), index), Country("ES", "Spain")).success.value
        .set(VatAmountCorrectionCountryPage(Index(1), index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removePeriodCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country("ES", "Spain")).success.value
          .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(10)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemovePeriodCorrectionPage(index).navigate(waypoints, expectedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must not change the answer and redirect to the next page when false is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val answers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(10)).success.value

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removePeriodCorrectionRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value
        val expectedAnswers = answers

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual RemovePeriodCorrectionPage(index).navigate(waypoints, expectedAnswers, expectedAnswers).url
        verify(mockSessionRepository, never()).set(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removePeriodCorrectionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemovePeriodCorrectionView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removePeriodCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removePeriodCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
