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
import forms.RemainingVatRateFromCountryFormProvider
import models.VatRateType.{Reduced, Standard}
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.RemainingVatRateFromCountryView

class RemainingVatRateFromCountryControllerSpec extends SpecBase with MockitoSugar {
  
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val formProvider = new RemainingVatRateFromCountryFormProvider()
  private val form: Form[Boolean] = formProvider()

  lazy val remainingVatRateFromCountryRoute: String = routes.RemainingVatRateFromCountryController.onPageLoad(waypoints, index, index).url

  val currentlyAnsweredVatRates: List[VatRateFromCountry] = List(
    VatRateFromCountry(BigDecimal(21.7), Standard, period.firstDay),
    VatRateFromCountry(BigDecimal(12.0), Reduced, period.firstDay)
  )

  val answers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index), currentlyAnsweredVatRates).success.value
    .set(SalesToCountryPage(index, index), 1).success.value
    .set(VatOnSalesPage(index, index), VatOnSales.Option1).success.value

  "RemainingVatRateFromCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers))
        .build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, index, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = answers.set(RemainingVatRateFromCountryPage(index, index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(true), waypoints, period, index, index, country)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, remainingVatRateFromCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = answers.set(RemainingVatRateFromCountryPage(index, index), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RemainingVatRateFromCountryPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, remainingVatRateFromCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, index, index, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no VAT rate sales data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, remainingVatRateFromCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
