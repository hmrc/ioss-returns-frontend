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
import forms.corrections.VatPayableForCountryFormProvider
import models.{Country, Index}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage, VatPayableForCountryPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.corrections.VatPayableForCountryView

class VatPayableForCountryControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new VatPayableForCountryFormProvider()
  private val form = formProvider(Country("DE", "Germany"), BigDecimal(1000))

  private lazy val vatPayableForCountryRoute = controllers.corrections.routes.VatPayableForCountryController.onPageLoad(waypoints, index, Index(0)).url

  "VatPayableForCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatPayableForCountryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, index, Index(0), Country("DE", "Germany"), period, BigDecimal(1000))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(VatPayableForCountryPage(index, Index(0)), true).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val view = application.injector.instanceOf[VatPayableForCountryView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(true),
          waypoints,
          index,
          Index(0),
          Country("DE", "Germany"),
          period,
          BigDecimal(1000)
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = userAnswers.set(VatPayableForCountryPage(index, Index(0)), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual VatPayableForCountryPage(index, Index(0)).navigate(waypoints, userAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(VatPayableForCountryPage(index, index), true).success.value
        .set(CorrectionCountryPage(index, index), Country("DE", "Germany")).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[VatPayableForCountryView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          waypoints,
          index,
          Index(0),
          Country("DE", "Germany"),
          period,
          BigDecimal(1000)
        )(
          request, messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no correction period or country found" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no correction period or country found" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
