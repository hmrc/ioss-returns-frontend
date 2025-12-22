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
import forms.corrections.CorrectionCountryFormProvider
import models.Country
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import repositories.SessionRepository
import services.CorrectionService
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionCountryView

class CorrectionCountryControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val formProvider = new CorrectionCountryFormProvider()
  private val form = formProvider(index, Seq.empty)
  private val country: Country = Arbitrary.arbitrary[Country].sample.value

  private val userAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), period).success.value

  private lazy val correctionCountryRoute: String = routes.CorrectionCountryController.onPageLoad(waypoints, index, index).url

  private val mockCorrectionService: CorrectionService = mock[CorrectionService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockCorrectionService)
  }

  "CorrectionCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), country).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val view = application.injector.instanceOf[CorrectionCountryView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(country), waypoints, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      when(mockCorrectionService.getAccumulativeVatForCountryTotalAmount(any(), any(), any())(any())) thenReturn (false, BigDecimal(0)).toFuture

      val mockSessionRepository = mock[SessionRepository]

      val updatedAnswers = emptyUserAnswers
        .set(CorrectionReturnPeriodPage(index), period).success.value
        .set(CorrectionCountryPage(index, index), country).success.value
        .set(
          PreviouslyDeclaredCorrectionAmountQuery(index, index),
          PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = BigDecimal(0))
        ).success.value

      when(mockSessionRepository.set(updatedAnswers)) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .overrides(
            bind[CorrectionService].toInstance(mockCorrectionService),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", country.code))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe CorrectionCountryPage(index, index).navigate(waypoints, emptyUserAnswers, updatedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, index, period, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionCountryRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
