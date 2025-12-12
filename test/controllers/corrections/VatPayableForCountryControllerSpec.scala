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
import forms.corrections.VatPayableForCountryFormProvider
import models.{Country, Index, UserAnswers}
import org.scalacheck.Arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage, VatPayableForCountryPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import views.html.corrections.VatPayableForCountryView

class VatPayableForCountryControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = Arbitrary.arbitrary[Country].sample.value

  private val formProvider = new VatPayableForCountryFormProvider()
  private val form = formProvider(country, BigDecimal(1000))

  private val baseUserAnswers: UserAnswers = emptyUserAnswers
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CorrectionCountryPage(index, index), country).success.value
    .set(
      PreviouslyDeclaredCorrectionAmountQuery(index, index),
      PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = BigDecimal(0))
    ).success.value

  private lazy val vatPayableForCountryRoute = controllers.corrections.routes.VatPayableForCountryController.onPageLoad(waypoints, index, Index(0)).url

  "VatPayableForCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = baseUserAnswers
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatPayableForCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, Index(0), country, period, BigDecimal(1000), isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there is a previously declared country" in {

      val userAnswers = baseUserAnswers
        .set(
          PreviouslyDeclaredCorrectionAmountQuery(index, index),
          PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = BigDecimal(1500))
        ).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(-1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[VatPayableForCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, Index(0), country, period, BigDecimal(500), isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = baseUserAnswers
        .set(VatPayableForCountryPage(index, Index(0)), true).success.value
        .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(1000)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val view = application.injector.instanceOf[VatPayableForCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          form.fill(true),
          waypoints,
          period,
          index,
          Index(0),
          country,
          period,
          BigDecimal(1000),
          isIntermediary = false,
          companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val userAnswers = baseUserAnswers
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

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe VatPayableForCountryPage(index, Index(0)).navigate(waypoints, userAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = baseUserAnswers
        .set(VatPayableForCountryPage(index, index), true).success.value
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

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(
          boundForm,
          waypoints,
          period,
          index,
          Index(0),
          country,
          period,
          BigDecimal(1000),
          isIntermediary = false,
          companyName = "Company Name"
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

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a GET if no correction period or country found" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, vatPayableForCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no correction period or country found" in {

      val application = applicationBuilder(Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, vatPayableForCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
