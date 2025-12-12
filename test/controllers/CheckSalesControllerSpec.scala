/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.CheckSalesFormProvider
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CheckSalesPage, JourneyRecoveryPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.RemainingVatRatesFromCountryQuery
import repositories.SessionRepository
import services.VatRateService
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.CheckSalesSummary
import viewmodels.govuk.SummaryListFluency
import views.html.CheckSalesView

class CheckSalesControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val mockVatRateService: VatRateService = mock[VatRateService]

  private val formProvider = new CheckSalesFormProvider()
  private val form: Form[Boolean] = formProvider()

  private val vatRatesFromCountry = Gen.listOfN(3, arbitrary[VatRateFromCountry]).sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value
  private val vatOnSalesValue: VatOnSales = arbitraryVatOnSales.arbitrary.sample.value

  private val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index, index), vatOnSalesValue).success.value

  private val vatRateFromCountry = Gen.listOfN(1, arbitrary[VatRateFromCountry]).sample.value
  private val  remainingVatRateForCountry = List(arbitrary[VatRateFromCountry].sample.value)

  private val completeAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index, index), vatRateFromCountry).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index, index), vatOnSalesValue).success.value

  private lazy val checkSalesRoute: String = CheckSalesPage(index, Some(index)).route(waypoints).url
  private lazy val postCheckSalesRoute: String = controllers.routes.CheckSalesController.onSubmit(waypoints, index, incompletePromptShown = false).url

  override def beforeEach(): Unit = {
    reset(mockVatRateService)
    super.beforeEach()
  }

  "CheckSales Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRateForCountry.toFuture

      val application = applicationBuilder(userAnswers = Some(completeAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkSalesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckSalesView]

        val list = CheckSalesSummary.rows(completeAnswers, waypoints, index)

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(form, waypoints, period, list, index, country, canAddAnotherVatRate = true, companyName = "CompanyName", isIntermediary = false)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the correct next page when valid data is submitted with only one VAT rate remaining" in {

      val vatRatesFromCountry = Gen.listOfN(3, arbitrary[VatRateFromCountry]).sample.value
      val remainingVatRateForCountry = List(arbitrary[VatRateFromCountry].sample.value)
      val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value

      val answers: UserAnswers = emptyUserAnswers
        .set(SoldGoodsPage, true).success.value
        .set(SoldToCountryPage(index), country).success.value
        .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value
        .set(SalesToCountryPage(index, index), salesValue).success.value
        .set(VatOnSalesPage(index, index), vatOnSalesValue).success.value
        .set(SalesToCountryPage(index, index. +(1)), salesValue).success.value
        .set(VatOnSalesPage(index, index. +(1)), vatOnSalesValue).success.value
        .set(SalesToCountryPage(index, index. +(2)), salesValue).success.value
        .set(VatOnSalesPage(index, index. +(2)), vatOnSalesValue).success.value

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRateForCountry.toFuture

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, postCheckSalesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = answers
          .set(RemainingVatRatesFromCountryQuery(index), remainingVatRateForCountry).success.value
          .set(CheckSalesPage(index, Some(index. +(2))), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CheckSalesPage(index, Some(index. +(2))).navigate(waypoints, answers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRateForCountry.toFuture

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(completeAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, postCheckSalesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        val expectedAnswers = completeAnswers
          .set(RemainingVatRatesFromCountryQuery(index), remainingVatRateForCountry).success.value
          .set(CheckSalesPage(index, Some(index)), true).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CheckSalesPage(index, Some(index)).navigate(waypoints, baseAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRateForCountry.toFuture

      val application = applicationBuilder(userAnswers = Some(completeAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request =
          FakeRequest(POST, postCheckSalesRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CheckSalesView]

        val result = route(application, request).value

        val list = CheckSalesSummary.rows(completeAnswers, waypoints, index)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(boundForm, waypoints, period, list, index, country, canAddAnotherVatRate = true, List.empty, "Company Name", false)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkSalesRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no sales data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkSalesRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, postCheckSalesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
