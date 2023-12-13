/*
 * Copyright 2023 HM Revenue & Customs
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
import forms.corrections.CorrectionListCountriesFormProvider
import models.Country
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionListCountriesPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import viewmodels.checkAnswers.corrections.CorrectionListCountriesSummary
import views.html.corrections.CorrectionListCountriesView

import scala.concurrent.Future

class CorrectionListCountriesControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new CorrectionListCountriesFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val correctionListCountriesRoute: String = controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, index).url

  private val country = arbitrary[Country].sample.value

  private val baseAnswers = emptyUserAnswers
    .set(CorrectionCountryPage(index, index), country).success.value

  "CorrectionListCountries Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionListCountriesView]
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          waypoints,
          list,
          period,
          index,
          canAddCountries = true
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val view = application.injector.instanceOf[CorrectionListCountriesView]

        val result = route(application, request).value
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          waypoints,
          list,
          period,
          index,
          canAddCountries = true
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, correctionListCountriesRoute).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionListCountriesPage(index), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionListCountriesPage(index, Some(index)).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionListCountriesRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionListCountriesView]

        val result = route(application, request).value
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          waypoints,
          list,
          period,
          index,
          canAddCountries = true
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionListCountriesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
