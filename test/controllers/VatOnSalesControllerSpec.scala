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
import forms.VatOnSalesFormProvider
import models.{Country, VatOnSales, VatOnSalesChoice, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, SalesToCountryPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import utils.FutureSyntax.FutureOps
import views.html.VatOnSalesView

class VatOnSalesControllerSpec extends SpecBase with MockitoSugar {

  lazy val vatOnSalesRoute: String = routes.VatOnSalesController.onPageLoad(waypoints, index, vatRateIndex).url
  val vatRateService: VatRateService = mock[VatRateService]
  val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  val netSales: BigDecimal = BigDecimal(400)
  val standardVatOnSales: BigDecimal = arbitrary[BigDecimal].sample.value
  val country: Country = arbitraryCountry.arbitrary.sample.value
  val validAnswer: BigDecimal = BigDecimal(400)
  private val validVatOnSales = VatOnSales(VatOnSalesChoice.Standard, standardVatOnSales)

  val formProvider = new VatOnSalesFormProvider(vatRateService)
  val form: Form[VatOnSales] = formProvider.apply(vatRateFromCountry, netSales)

  "VatOnSales Controller" - {

    "must return OK and the correct view for a GET" in {
      when(vatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales
      val userAnswers = for {
        answer1 <- emptyUserAnswers.set(SoldToCountryPage(index), country)
        answer2 <- answer1.set(VatRatesFromCountryPage(index, index), List(vatRateFromCountry))
        answer3 <- answer2.set(SalesToCountryPage(index, vatRateIndex), validAnswer)
      } yield answer3
      val application = applicationBuilder(userAnswers = Some(userAnswers.success.value))
        .overrides(bind[VatRateService].toInstance(vatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatOnSalesView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, index, vatRateIndex, country, vatRateFromCountry, netSales, standardVatOnSales)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(vatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales
      val userAnswers = for {
        answer1 <- emptyUserAnswers.set(SoldToCountryPage(index), country)
        answer2 <- answer1.set(VatRatesFromCountryPage(index, index), List(vatRateFromCountry))
        answer3 <- answer2.set(SalesToCountryPage(index, vatRateIndex), validAnswer)
        answer4 <- answer3.set(VatOnSalesPage(index, vatRateIndex), validVatOnSales)
      } yield answer4

      val application = applicationBuilder(userAnswers = Some(userAnswers.success.value))
        .overrides(bind[VatRateService].toInstance(vatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesRoute)

        val view = application.injector.instanceOf[VatOnSalesView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validVatOnSales), waypoints, period, index, vatRateIndex, country, vatRateFromCountry, netSales, standardVatOnSales)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
      when(vatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales

      val userAnswers = for {
        answer1 <- emptyUserAnswers.set(SoldToCountryPage(index), country)
        answer2 <- answer1.set(VatRatesFromCountryPage(index, index), List(vatRateFromCountry))
        answer3 <- answer2.set(SalesToCountryPage(index, vatRateIndex), validAnswer)
      } yield answer3
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(userAnswers.success.value))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .overrides(bind[VatRateService].toInstance(vatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesRoute)
            .withFormUrlEncodedBody(("choice", VatOnSalesChoice.values.head.toString))

        val result = route(application, request).value

        val expectedAnswers = userAnswers
          .map(_.set(VatOnSalesPage(index, vatRateIndex), validVatOnSales)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatOnSalesPage(index, vatRateIndex).navigate(waypoints, userAnswers.success.value, expectedAnswers.success.value).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers.success.value))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(vatRateService.standardVatOnSales(any(), any())) thenReturn standardVatOnSales
      val userAnswers = for {
        answer1 <- emptyUserAnswers.set(SoldToCountryPage(index), country)
        answer2 <- answer1.set(VatRatesFromCountryPage(index, index), List(vatRateFromCountry))
        answer3 <- answer2.set(SalesToCountryPage(index, vatRateIndex), validAnswer)
      } yield answer3
      val application = applicationBuilder(userAnswers = Some(userAnswers.success.value))
        .overrides(bind[VatRateService].toInstance(vatRateService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[VatOnSalesView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, index, vatRateIndex, country, vatRateFromCountry, netSales, standardVatOnSales)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[VatRateService].toInstance(vatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatOnSalesRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[VatRateService].toInstance(vatRateService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, vatOnSalesRoute)
            .withFormUrlEncodedBody(("value", VatOnSalesChoice.values.head.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
