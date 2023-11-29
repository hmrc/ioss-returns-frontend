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
import forms.SalesToCountryFormProvider
import models.{Country, VatRateFromCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{SalesToCountryPage, SoldToCountryPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import views.html.SalesToCountryView

import scala.concurrent.Future

class SalesToCountryControllerSpec extends SpecBase with MockitoSugar {

  private val mockVatRateService = mock[VatRateService]

  val validAnswer: BigDecimal = 1

  lazy val salesToCountryRoute: String = routes.SalesToCountryController.onPageLoad(waypoints, index, index).url

  private val country = arbitrary[Country].sample.value
  private val vatRate = arbitrary[VatRateFromCountry].sample.value

  val formProvider = new SalesToCountryFormProvider(mockVatRateService)
  val form: Form[BigDecimal] = formProvider(vatRate)

  private val baseAnswers =
    emptyUserAnswers
      .set(SoldToCountryPage(index), country).success.value
      .set(VatRatesFromCountryPage(index), List(vatRate)).success.value



  "SalesToCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesToCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SalesToCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, index, country, index, vatRate)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(SalesToCountryPage(index, index), validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, salesToCountryRoute)

        val view = application.injector.instanceOf[SalesToCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer),
          waypoints,
          period,
          index,
          country,
          index,
          vatRate
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
        val request =
          FakeRequest(POST, salesToCountryRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(SalesToCountryPage(index, index), validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.VatOnSalesController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, salesToCountryRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[SalesToCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, index, country, index, vatRate)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, salesToCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, salesToCountryRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
