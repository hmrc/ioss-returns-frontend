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
import controllers.routes
import forms.CorrectionReturnYearFormProvider
import models.{Index, Period}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnYearPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.corrections.CorrectionReturnYearView

import java.time.Month
import scala.concurrent.Future

class CorrectionReturnYearControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new CorrectionReturnYearFormProvider()
  private val form: Form[Period] = formProvider(index, Seq.empty)

  private val periodSequence = Seq(Period(2021, Month.OCTOBER), Period(2021, Month.DECEMBER), Period(2022, Month.MARCH))

  lazy val correctionReturnYearRoute: String = controllers.corrections.routes.CorrectionReturnYearController.onPageLoad(waypoints, index).url

  "CorrectionReturnYear Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnYearRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, periodSequence, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(CorrectionReturnYearPage(Index(0)), Period(2021, Month.OCTOBER)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnYearRoute)

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, waypoints, period, periodSequence, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

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
          FakeRequest(POST, correctionReturnYearRoute)
            .withFormUrlEncodedBody(("value", Period(2021, Month.OCTOBER).toString))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnYearPage(index), Period(2021, Month.OCTOBER)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnYearRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, periodSequence, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnYearRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnYearRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
