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
import forms.SoldToCountryListFormProvider
import models.{Country, Index, UserAnswers, VatOnSales, VatRatesFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryListPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.SoldToCountryListSummary
import views.html.SoldToCountryListView

class SoldToCountryListControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new SoldToCountryListFormProvider()
  val form: Form[Boolean] = formProvider()

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val salesValue: Int = 1234

  val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index), Set(VatRatesFromCountry.values.head)).success.value
    .set(SalesToCountryPage(index), salesValue).success.value
    .set(VatOnSalesPage(index), VatOnSales.values.head).success.value

  lazy val soldToCountryListRoute: String = routes.SoldToCountryListController.onPageLoad(waypoints).url

  "SoldToCountryList Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SoldToCountryListView]

        val list = SoldToCountryListSummary.addToListRows(baseAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = true)(request, messages(application)).toString
      }
    }

    // TODO
//    "must populate the view correctly on a GET when the question has previously been answered" in {
//
//      val userAnswers = emptyUserAnswers.set(SoldToCountryListPage(), true).success.value
//
//      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
//
//      running(application) {
//        implicit val msgs: Messages = messages(application)
//
//        val request = FakeRequest(GET, soldToCountryListRoute)
//
//        val result = route(application, request).value
//
//        val view = application.injector.instanceOf[SoldToCountryListView]
//
//        val list = SoldToCountryListSummary.row(baseAnswers, waypoints, SoldToCountryListPage())
//
//        status(result) mustBe OK
//        contentAsString(result) must not be view(form.fill(true), waypoints, period, list, canAddSalesToEuOrNi = true)(request, messages(application)).toString
//      }
//    }

    "must return OK and populate the view correctly on a GET when the maximum number of sold to countries have already been added" in {

      val userAnswers = (0 to Country.euCountriesWithNI.size).foldLeft(baseAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(SoldToCountryPage(Index(index)), country).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        
        val request = FakeRequest(GET, soldToCountryListRoute)

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(userAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = false)(request, messages(application)).toString
      }
    }

    "must return OK and populate the view correctly on a GET when just below the maximum number of sold to countries have been added" in {

      val userAnswers = (0 until (Country.euCountriesWithNI.size -1)).foldLeft(baseAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers.set(SoldToCountryPage(Index(index)), country).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(userAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = true)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, soldToCountryListRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.set(SoldToCountryListPage(), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe SoldToCountryListPage().navigate(waypoints, baseAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {

        val request =
          FakeRequest(POST, soldToCountryListRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(baseAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, list, canAddSales = true)(request, messages(application)).toString // TODO
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if user answers are empty" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, soldToCountryListRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
