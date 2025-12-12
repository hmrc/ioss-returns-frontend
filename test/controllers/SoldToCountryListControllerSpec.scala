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

package controllers

import base.SpecBase
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import forms.SoldToCountryListFormProvider
import models.{Country, Index, UserAnswers, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryListPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSalesWithTotalAndVatQuery, SalesToCountryWithOptionalSales}
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.SoldToCountryListSummary
import views.html.SoldToCountryListView

class SoldToCountryListControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new SoldToCountryListFormProvider()
  private val form: Form[Boolean] = formProvider()

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value

  private val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index, index), List[VatRateFromCountry](vatRateFromCountry)).success.value
    .set(SalesToCountryPage(index, vatRateIndex), salesValue).success.value
    .set(VatOnSalesPage(index, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value).success.value

  private lazy val soldToCountryListRoute: String = routes.SoldToCountryListController.onPageLoad(waypoints).url
  private lazy val soldToCountryPostRoute: String = routes.SoldToCountryListController.onSubmit(waypoints, incompletePromptShown = false).url
  private lazy val soldToCountryPostRouteTrue: String = routes.SoldToCountryListController.onSubmit(waypoints, incompletePromptShown = true).url

  "SoldToCountryList Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SoldToCountryListView]

        val list = SoldToCountryListSummary.addToListRows(baseAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = true, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseAnswers.set(SoldToCountryListPage(), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SoldToCountryListView]

        val list = SoldToCountryListSummary.addToListRows(baseAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) must not be view(form.fill(true), waypoints, period, list, canAddSales = true, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and populate the view correctly on a GET when the maximum number of sold to countries have already been added" in {

      val userAnswers = (0 to Country.euCountriesWithNI.size).foldLeft(baseAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(SoldToCountryPage(Index(index)), country).success.value
          .set(VatRatesFromCountryPage(Index(index), Index(index)), List[VatRateFromCountry](vatRateFromCountry)).success.value
          .set(SalesToCountryPage(Index(index), vatRateIndex), salesValue).success.value
          .set(VatOnSalesPage(Index(index), vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        
        val request = FakeRequest(GET, soldToCountryListRoute)

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(userAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = false, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and populate the view correctly on a GET when just below the maximum number of sold to countries have been added" in {

      val userAnswers = (0 until (Country.euCountriesWithNI.size -1)).foldLeft(baseAnswers) { (userAnswers: UserAnswers, index: Int) =>
        userAnswers
          .set(SoldToCountryPage(Index(index)), country).success.value
          .set(VatRatesFromCountryPage(Index(index), Index(index)), List[VatRateFromCountry](vatRateFromCountry)).success.value
          .set(SalesToCountryPage(Index(index), vatRateIndex), salesValue).success.value
          .set(VatOnSalesPage(Index(index), vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value).success.value
      }

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(userAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, list, canAddSales = true, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
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
          FakeRequest(POST, soldToCountryPostRoute)
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
          FakeRequest(POST, soldToCountryPostRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SoldToCountryListView]

        val result = route(application, request).value

        val list = SoldToCountryListSummary.addToListRows(baseAnswers, waypoints, SoldToCountryListPage())

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, list, canAddSales = true, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
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
          FakeRequest(POST, soldToCountryPostRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must return OK and the correct view for a GET with incomplete sales data" in {

      val userAnswers = baseAnswers
        .set(SoldToCountryPage(Index(0)), country).success.value
        .remove(SalesToCountryPage(Index(0), vatRateIndex)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, soldToCountryListRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SoldToCountryListView]

        val salesSummary = SoldToCountryListSummary.addToListRows(userAnswers, waypoints, SoldToCountryListPage())
        val incompleteCountries = Seq(country)

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          form,
          waypoints,
          period,
          salesSummary,
          canAddSales = true,
          incompleteCountries,
          isIntermediary = false,
          companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must redirect to VatRatesFromCountryController when incompletePromptShown is true and the first incomplete country has no VAT rates" in {

      val salesToCountryWithNoVatRates = SalesToCountryWithOptionalSales(country, None)

      val userAnswers = emptyUserAnswers.set(AllSalesWithTotalAndVatQuery, List(salesToCountryWithNoVatRates)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, soldToCountryPostRouteTrue)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.VatRatesFromCountryController.onPageLoad(waypoints, Index(0)).url
      }
    }

    "must redirect to CheckSalesController when incompletePromptShown is true and the first incomplete country has VAT rates" in {

      val userAnswers = baseAnswers
        .set(SoldToCountryPage(Index(0)), country).success.value
        .set(VatRatesFromCountryPage(Index(0), Index(0)), List(vatRateFromCountry)).success.value
        .remove(SalesToCountryPage(Index(0), vatRateIndex)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, soldToCountryPostRouteTrue)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.CheckSalesController.onPageLoad(waypoints, Index(0)).url
      }
    }

    "must redirect to SoldToCountryListController when incompletePromptShown is false" in {

      val userAnswers = baseAnswers
        .set(SoldToCountryPage(Index(0)), country).success.value
        .remove(SalesToCountryPage(Index(0), vatRateIndex)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, soldToCountryPostRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.SoldToCountryListController.onPageLoad(waypoints).url
      }
    }
  }
}
