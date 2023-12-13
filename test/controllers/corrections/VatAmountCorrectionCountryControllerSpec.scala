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

package controllers.corrections

import base.SpecBase
import forms.corrections.VatAmountCorrectionCountryFormProvider
import models.Country
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.{times, verify}
import org.mockito.{ArgumentMatchersSugar, Mockito}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, UndeclaredCountryCorrectionPage, VatAmountCorrectionCountryPage, VatPayableForCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.corrections.VatAmountCorrectionCountryView

import scala.concurrent.Future

class VatAmountCorrectionCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockSessionRepository = mock[SessionRepository]

  private val selectedCountry = arbitrary[Country].sample.value

  private val formProvider = new VatAmountCorrectionCountryFormProvider()
  private val form = formProvider(selectedCountry.name)
  private val userAnswersWithCountryAndPeriod = emptyUserAnswers.set(CorrectionCountryPage(index, index), selectedCountry).flatMap(_.set(UndeclaredCountryCorrectionPage(index, index), true)).success.value

  private val validAnswer = BigDecimal(10)

  private lazy val countryVatCorrectionRoute =
    VatAmountCorrectionCountryPage(index, index).route(waypoints).url

  override def beforeEach(): Unit = {
    Mockito.reset(mockSessionRepository)
  }

  "VatAmountCorrectionCountry Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, waypoints, period, index, index, selectedCountry
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers =
        userAnswersWithCountryAndPeriod.set(VatAmountCorrectionCountryPage(index, index), validAnswer).success.value


      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(validAnswer), waypoints, period, index, index, selectedCountry
        )(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value
        val expectedAnswers =
          userAnswersWithCountryAndPeriod.set(VatAmountCorrectionCountryPage(index, index), validAnswer).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatPayableForCountryPage(index, index).route(waypoints).url

        verify(mockSessionRepository, times(1)).set(ArgumentMatchersSugar.eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCountryAndPeriod))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[VatAmountCorrectionCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm, waypoints, period, index, index, selectedCountry
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, countryVatCorrectionRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no correction period or country found in user answers" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, countryVatCorrectionRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
