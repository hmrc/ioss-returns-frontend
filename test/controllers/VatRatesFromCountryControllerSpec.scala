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

package controllers

import base.SpecBase
import forms.VatRatesFromCountryFormProvider
import models.{Country, VatRatesFromCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{SoldToCountryPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.VatRatesFromCountryView

import scala.concurrent.Future

class VatRatesFromCountryControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val userAnswersWithCountry = emptyUserAnswers.set(SoldToCountryPage(index), country).success.value

  lazy val vatRatesFromCountryRoute: String = routes.VatRatesFromCountryController.onPageLoad(waypoints, index).url

  val formProvider = new VatRatesFromCountryFormProvider()
  val form: Form[Set[VatRatesFromCountry]] = formProvider()

  "VatRatesFromCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry)).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, waypoints, period, index, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCountry.set(VatRatesFromCountryPage(index), VatRatesFromCountry.values.toSet).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form.fill(VatRatesFromCountry.values.toSet),
            waypoints,
            period,
            index,
            country
          )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountry))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value[0]", VatRatesFromCountry.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SalesToCountryController.onPageLoad(waypoints, index).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry)).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, index, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value[0]", VatRatesFromCountry.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
