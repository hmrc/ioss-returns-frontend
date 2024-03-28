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

package controllers.payments

import base.SpecBase
import forms.payments.WhichPreviousRegistrationVatPeriodToPayFormProvider
import models.Period
import models.payments.{Payment, PaymentStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.payments.WhichPreviousRegistrationVatPeriodToPayPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.payments.WhichPreviousRegistrationVatPeriodToPayView

import java.time.LocalDate
import scala.concurrent.Future

class WhichPreviousRegistrationVatPeriodToPayControllerSpec extends SpecBase with MockitoSugar {

  // TODO
  private val formProvider = new WhichPreviousRegistrationVatPeriodToPayFormProvider()
  private val form: Form[Period] = formProvider()
  
  private val payments: List[Payment] = List(Payment(period, BigDecimal(100.00), LocalDate.now, PaymentStatus.Unpaid))

  private lazy val whichPreviousRegistrationVatPeriodToPayRoute = routes.WhichPreviousRegistrationVatPeriodToPayController.onPageLoad(waypoints).url

  "WhichPreviousRegistrationVatPeriodToPay Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, payments, paymentError = false)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(WhichPreviousRegistrationVatPeriodToPayPage, period).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) mustBe view(form.fill(period), waypoints, payments, paymentError = false)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      // use new repo
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhichPreviousRegistrationVatPeriodToPayView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, payments, paymentError = false)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whichPreviousRegistrationVatPeriodToPayRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whichPreviousRegistrationVatPeriodToPayRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
