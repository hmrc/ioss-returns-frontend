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
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import forms.DeleteVatRateSalesForCountryFormProvider
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verifyNoInteractions, when}
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.VatRateFromCountryQuery
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.DeleteVatRateSalesForCountryView

class DeleteVatRateSalesForCountryControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value
  private val vatOnSalesValue: VatOnSales = arbitraryVatOnSales.arbitrary.sample.value
  private val vatRate = vatRateFromCountry.rateForDisplay

  private val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index, index), List[VatRateFromCountry](vatRateFromCountry)).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index, index), vatOnSalesValue).success.value

  private val formProvider = new DeleteVatRateSalesForCountryFormProvider()
  private val form: Form[Boolean] = formProvider(vatRate, country)

  private lazy val deleteVatRateSalesForCountryRoute: String = routes.DeleteVatRateSalesForCountryController.onPageLoad(waypoints, index, index).url

  "DeleteVatRateSalesForCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteVatRateSalesForCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteVatRateSalesForCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, index, index, vatRate, country)(request, messages(application)).toString
      }
    }

    "must delete a record and then redirect to the mini check your answer page (even if there is no vat sales in user answers after the deletion), when Yes is submitted" in {

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
          FakeRequest(POST, deleteVatRateSalesForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = baseAnswers.remove(VatRateFromCountryQuery(index, index)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteVatRateSalesForCountryPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must delete a record and then redirect to the mini check your answer page when Yes is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val updatedAnswer = baseAnswers.set(VatRatesFromCountryPage(index, index + 1), List[VatRateFromCountry](vatRateFromCountry)).success.value
        .set(SalesToCountryPage(index, index + 1), salesValue).success.value
        .set(VatOnSalesPage(index, index + 1), vatOnSalesValue).success.value

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswer))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteVatRateSalesForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = baseAnswers.remove(VatRateFromCountryQuery(index, index)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteVatRateSalesForCountryPage(index, index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must not delete a record and then redirect to the next page when No is submitted" in {

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
          FakeRequest(POST, deleteVatRateSalesForCountryRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteVatRateSalesForCountryPage(index, index).navigate(waypoints, baseAnswers, baseAnswers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteVatRateSalesForCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteVatRateSalesForCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, index, index, vatRate, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteVatRateSalesForCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no VAT rate sales data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteVatRateSalesForCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteVatRateSalesForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
