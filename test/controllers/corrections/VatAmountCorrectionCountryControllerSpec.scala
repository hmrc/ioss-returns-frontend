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

package controllers.corrections

import base.SpecBase
import forms.corrections.VatAmountCorrectionCountryFormProvider
import models.Country
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.mockito.Mockito.{times, verify}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.corrections.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.corrections.VatAmountCorrectionCountryView

class VatAmountCorrectionCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSessionRepository = mock[SessionRepository]

  private val selectedCountry: Country = arbitrary[Country].sample.value
  private val minimumCorrection: BigDecimal = arbitraryBigDecimal.arbitrary.sample.value

  private val formProvider = new VatAmountCorrectionCountryFormProvider()
  private val form = formProvider(selectedCountry.name, minimumCorrection)

  private val validAnswer = BigDecimal(10)
  private val validAnswerZero = BigDecimal(0)

  private val userAnswersWithPreviouslyUndeclaredCountry = emptyUserAnswers
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CorrectionCountryPage(index, index), selectedCountry).flatMap(_.set(UndeclaredCountryCorrectionPage(index, index), true)).success.value
    .set(
      PreviouslyDeclaredCorrectionAmountQuery(index, index),
      PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = validAnswerZero)
    ).success.value

  private val userAnswersWithPreviouslyDeclaredCountry = emptyUserAnswers
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CorrectionCountryPage(index, index), selectedCountry).flatMap(_
      .set(
        PreviouslyDeclaredCorrectionAmountQuery(index, index),
        PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = minimumCorrection)
      )
    ).success.value

  private lazy val countryVatCorrectionRoute = VatAmountCorrectionCountryPage(index, index).route(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockSessionRepository)
  }

  "VatAmountCorrectionCountry Controller" - {

    "must return OK and the correct view for a GET on a previously undeclared country" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPreviouslyUndeclaredCountry))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, period, index, selectedCountry, isCountryPreviouslyDeclared = false, validAnswer, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET on a previously declared country" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPreviouslyDeclaredCountry))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, period, index, selectedCountry, isCountryPreviouslyDeclared = true, minimumCorrection, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET on a previously declared country with a correction amount of ZERO" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPreviouslyDeclaredCountry
        .set(
          PreviouslyDeclaredCorrectionAmountQuery(index, index),
          PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = validAnswerZero))
        .success.value
      )).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, period, index, selectedCountry, isCountryPreviouslyDeclared = true, validAnswerZero, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = userAnswersWithPreviouslyUndeclaredCountry
        .set(VatAmountCorrectionCountryPage(index, index), validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          form.fill(validAnswer), waypoints, period, index, period, index, selectedCountry, isCountryPreviouslyDeclared = false, minimumCorrection, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithPreviouslyUndeclaredCountry))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value
        val expectedAnswers = userAnswersWithPreviouslyUndeclaredCountry
          .set(VatAmountCorrectionCountryPage(index, index), validAnswer).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe VatPayableForCountryPage(index, index).route(waypoints).url

        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithPreviouslyUndeclaredCountry))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(
          boundForm, waypoints, period, index, period, index, selectedCountry, isCountryPreviouslyDeclared = false, minimumCorrection, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
