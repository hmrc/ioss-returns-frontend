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

package controllers.corrections

import base.SpecBase
import controllers.routes
import forms.corrections.RemoveCountryCorrectionFormProvider
import models.{Country, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, RemoveCountryCorrectionPage, VatAmountCorrectionCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.{CorrectionPeriodQuery, CorrectionToCountryQuery}
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.corrections.RemoveCountryCorrectionView

class RemoveCountryCorrectionControllerSpec extends SpecBase with MockitoSugar {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val formProvider = new RemoveCountryCorrectionFormProvider()
  private val form: Form[Boolean] = formProvider(country)

  private lazy val removeCountryCorrectionRoute: String =
    controllers.corrections.routes.RemoveCountryCorrectionController.onPageLoad(waypoints, index, index).url

  private val answers: UserAnswers = completedUserAnswersWithCorrections
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(CorrectionCountryPage(index, index), country).success.value

  "RemoveCountryCorrection Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request = FakeRequest(GET, removeCountryCorrectionRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[RemoveCountryCorrectionView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, index, country, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when the answer is true for a single country" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(answers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = answers
          .remove(CorrectionToCountryQuery(index, index)).success.value
          .remove(CorrectionPeriodQuery(index)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RemoveCountryCorrectionPage(index, index).navigate(waypoints, answers, expectedAnswers).url
      }
    }

    "must save the answer and redirect to the next page when the answer is true and there are multiple countries" in {

      val additionalCountry: Country = arbitraryCountry.arbitrary.suchThat(_ != country).sample.value

      val multipleAnswers: UserAnswers = answers
        .set(CorrectionCountryPage(index, index + 1), additionalCountry).success.value
        .set(VatAmountCorrectionCountryPage(index, index + 1), arbitraryBigDecimal.arbitrary.sample.value).success.value

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(multipleAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = multipleAnswers
          .remove(CorrectionToCountryQuery(index, index)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe RemoveCountryCorrectionPage(index, index).navigate(waypoints, multipleAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(answers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[RemoveCountryCorrectionView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, index, index, country, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeCountryCorrectionRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, removeCountryCorrectionRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
