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
import forms.RemainingVatRateFromCountryFormProvider
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages._
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.AllVatRatesFromCountryQuery
import repositories.SessionRepository
import services.VatRateService
import utils.FutureSyntax.FutureOps
import views.html.RemainingVatRateFromCountryView

class RemainingVatRateFromCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val formProvider = new RemainingVatRateFromCountryFormProvider()
  private val form: Form[Boolean] = formProvider()

  private val vatRateFromCountry1: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val vatRateFromCountry2: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val remainingVatRate: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value

  val currentlyAnsweredVatRates: List[VatRateFromCountry] = List(
    vatRateFromCountry1,
    vatRateFromCountry2
  )

  val answers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index), currentlyAnsweredVatRates).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index, index), VatOnSales.Option1).success.value

  lazy val remainingVatRateFromCountryRoute: String = routes.RemainingVatRateFromCountryController.onPageLoad(waypoints, index, index).url

  private val mockVatRateService = mock[VatRateService]

  override def beforeEach: Unit = {
    reset(mockVatRateService)
  }

  "RemainingVatRateFromCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates)) thenReturn Seq(remainingVatRate)

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(form, waypoints, period, index, index, remainingVatRate.rateForDisplay, country)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates)) thenReturn Seq(remainingVatRate)

      val userAnswers = answers.set(RemainingVatRateFromCountryPage(index, index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, remainingVatRateFromCountryRoute)

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        val result = route(application, request).value

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(form.fill(true), waypoints, period, index, index, remainingVatRate.rateForDisplay, country)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates)) thenReturn Seq(remainingVatRate)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, remainingVatRateFromCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = answers
          .set(RemainingVatRateFromCountryPage(index, index), true).success.value
          .set(AllVatRatesFromCountryQuery(index), currentlyAnsweredVatRates ++ Seq(remainingVatRate)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RemainingVatRateFromCountryPage(index, index).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates)) thenReturn Seq(remainingVatRate)

      val application = applicationBuilder(userAnswers = Some(answers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, remainingVatRateFromCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemainingVatRateFromCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST

        contentAsString(result) mustBe
          view(boundForm, waypoints, period, index, index, remainingVatRate.rateForDisplay, country)(request, messages(application)).toString
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
