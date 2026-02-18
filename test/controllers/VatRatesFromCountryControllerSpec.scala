/*
 * Copyright 2026 HM Revenue & Customs
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
import models.{Country, VatOnSales, VatOnSalesChoice, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{JourneyRecoveryPage, SalesToCountryPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.{AllSalesByCountryQuery, VatRateWithOptionalSalesFromCountry}
import repositories.SessionRepository
import services.VatRateService
import utils.FutureSyntax.FutureOps
import views.html.VatRatesFromCountryView

class VatRatesFromCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val vatRatesFromCountry = Gen.listOfN(3,  arbitrary[VatRateFromCountry]).sample.value
  private val userAnswersWithCountry = emptyUserAnswers
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value

  private val formProvider = new VatRatesFromCountryFormProvider()
  private val form: Form[List[VatRateFromCountry]] = formProvider(vatRatesFromCountry)

  private val mockVatRateService = mock[VatRateService]

  private lazy val vatRatesFromCountryRoute: String = routes.VatRatesFromCountryController.onPageLoad(waypoints, index).url

  override def beforeEach(): Unit = {
    reset(mockVatRateService)
  }

  "VatRatesFromCountry Controller" - {

    "must save the answer and redirect to the correct next page for a GET when there's only one VAT rate" in {

      val vatRatesFromCountry = Gen.listOfN(1, arbitrary[VatRateFromCountry]).sample.value
      val remainingVatRateForCountry = List(arbitrary[VatRateFromCountry].sample.value)
      val userAnswersWithCountry = emptyUserAnswers
        .set(SoldToCountryPage(index), country).success.value
        .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRateForCountry.toFuture
      when(mockVatRateService.vatRates(any(), any())(any())) thenReturn vatRatesFromCountry.toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe routes.RemainingVatRateFromCountryController.onPageLoad(waypoints, index, index. +(1)).url
      }
    }

    "must return OK and the correct view for a GET when there are multiple VAT rates remaining" in {

      val vatRatesFromCountry = Gen.listOfN(3, arbitrary[VatRateFromCountry]).sample.value
      val remainingVatRates = Gen.listOfN(2, arbitrary[VatRateFromCountry]).suchThat(x => !vatRatesFromCountry.contains(x)).sample.value
      val userAnswersWithCountry = emptyUserAnswers
        .set(SoldToCountryPage(index), country).success.value
        .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRates.toFuture
      when(mockVatRateService.vatRates(any(), any())(any())) thenReturn vatRatesFromCountry.toFuture

      val preppedForm = formProvider(vatRatesFromCountry).fill(vatRatesFromCountry)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountry))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(preppedForm, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRatesFromCountry), isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn vatRatesFromCountry.toFuture
      when(mockVatRateService.vatRates(any(), any())(any())) thenReturn vatRatesFromCountry.toFuture

      val userAnswers = userAnswersWithCountry.set(VatRatesFromCountryPage(index, index), vatRatesFromCountry).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val view = application.injector.instanceOf[VatRatesFromCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(
            form.fill(vatRatesFromCountry),
            waypoints,
            period,
            index,
            country,
            utils.ItemsHelper.checkboxItems(vatRatesFromCountry),
            isIntermediary = false,
            companyName = "Company Name"
          )(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {

      val currentAnswers = userAnswersWithCountry
        .set(VatRatesFromCountryPage(index, index), vatRatesFromCountry.tail).success.value
        .set(SalesToCountryPage(index, index), BigDecimal(100)).success.value
        .set(VatOnSalesPage(index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(50))).success.value
        .set(SalesToCountryPage(index, index + 1), BigDecimal(2000)).success.value
        .set(VatOnSalesPage(index, index + 1), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(300))).success.value

      val remainingVatRate = Seq(
        VatRateFromCountry(
          rate = vatRatesFromCountry.head.rate,
          rateType = vatRatesFromCountry.head.rateType,
          validFrom = vatRatesFromCountry.head.validFrom,
          validUntil = vatRatesFromCountry.head.validUntil
        )
      )

      val mockSessionRepository = mock[SessionRepository]

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn remainingVatRate.toFuture
      when(mockVatRateService.vatRates(any(), any())(any())) thenReturn vatRatesFromCountry.toFuture
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(currentAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .overrides(bind[VatRateService].toInstance(mockVatRateService))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(
              ("value[0]", vatRatesFromCountry.head.rate.toString),
              ("value[1]", vatRatesFromCountry(1).rate.toString),
              ("value[2]", vatRatesFromCountry(2).rate.toString)
            )

        val result = route(application, request).value

        val currentVatRateAnswers = currentAnswers
          .get(AllSalesByCountryQuery(index)).get

        val currentVatRatesWithAmounts = currentVatRateAnswers.vatRatesFromCountry.toList.flatten

        val additionVatRateOptionalValue = VatRateWithOptionalSalesFromCountry(
          vatRatesFromCountry.head.rate,
          vatRatesFromCountry.head.rateType,
          vatRatesFromCountry.head.validFrom,
          vatRatesFromCountry.head.validUntil,
          salesAtVatRate = None
        )

        val expectedVatRatesAnswers = currentVatRateAnswers.copy(vatRatesFromCountry = Some(currentVatRatesWithAmounts ++ Seq(additionVatRateOptionalValue)))

        val expectedAnswers = currentAnswers
          .set(AllSalesByCountryQuery(index), expectedVatRatesAnswers).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe VatRatesFromCountryPage(index, index. +(2)).navigate(waypoints, currentAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockVatRateService.getRemainingVatRatesForCountry(any(), any(), any())(any())) thenReturn vatRatesFromCountry.toFuture
      when(mockVatRateService.vatRates(any(), any())(any())) thenReturn vatRatesFromCountry.toFuture

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

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(boundForm, waypoints, period, index, country, utils.ItemsHelper.checkboxItems(vatRatesFromCountry), isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no VAT rate data is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vatRatesFromCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatRatesFromCountryRoute)
            .withFormUrlEncodedBody(("value[0]", vatRatesFromCountry.head.rate.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
