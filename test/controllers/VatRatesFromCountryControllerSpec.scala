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
import models.{Country, VatRateFromCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{SoldToCountryPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import views.html.VatRatesFromCountryView

import scala.concurrent.Future

class VatRatesFromCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val userAnswersWithCountry = emptyUserAnswers.set(SoldToCountryPage(index), country).success.value
  private val vatRatesFromCountry = List(arbitrary[VatRateFromCountry].sample.value, arbitrary[VatRateFromCountry].sample.value)
  private lazy val vatRatesFromCountryRoute: String = routes.VatRatesFromCountryController.onPageLoad(waypoints, index).url

  private val formProvider = new VatRatesFromCountryFormProvider()
  private val form: Form[List[VatRateFromCountry]] = formProvider(vatRatesFromCountry)

  private val mockVatRateService = mock[VatRateService]

  override def beforeEach: Unit = {
    reset(mockVatRateService)
  }

  "VatRatesFromCountry Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRatesFromCountry))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      val userAnswers = userAnswersWithCountry.set(VatRatesFromCountryPage(index), vatRatesFromCountry).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(
            form.fill(vatRatesFromCountry),
            waypoints,
            period,
            index,
            country,
            utils.ItemsHelper.checkboxItems(vatRatesFromCountry)
          )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountry))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value[0]", vatRatesFromCountry.head.rate.toString), ("value[1]", vatRatesFromCountry(1).rate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SalesToCountryController.onPageLoad(waypoints, index, vatRateIndex).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRatesFromCountry))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      when(mockVatRateService.vatRates(any(), any())) thenReturn vatRatesFromCountry
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value[0]", vatRatesFromCountry.head.rate.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "must skip this page (303 SEE_OTHER) if there is only 1 selection and update user answers" in {
    val singleVatRate = vatRatesFromCountry.drop(1)
    when(mockVatRateService.vatRates(any(), any())) thenReturn singleVatRate
    val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry))
      .overrides(bind[VatRateService].toInstance(mockVatRateService))
      .build()

    running(application) {
      val request = FakeRequest(GET, vatRatesFromCountryRoute)
      val updatedAnswers = userAnswersWithCountry.set(VatRatesFromCountryPage(index), singleVatRate).success.value

      val result = route(application, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustEqual VatRatesFromCountryPage(index).navigate(waypoints, userAnswersWithCountry, updatedAnswers).url

    }

  }
}
