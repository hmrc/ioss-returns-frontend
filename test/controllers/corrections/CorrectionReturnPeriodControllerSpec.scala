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
import forms.corrections.CorrectionReturnPeriodFormProvider
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import models.{Index, Period, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionReturnPeriodPage, CorrectionReturnYearPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ObligationsService
import utils.ConvertPeriodKey
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionReturnPeriodView

import java.time._
import scala.concurrent.Future

class CorrectionReturnPeriodControllerSpec extends SpecBase with MockitoSugar {

  private val date: LocalDate = LocalDate.of(2026, 5, 13)
  private val instant: Instant = date.atStartOfDay(ZoneId.systemDefault).toInstant
  private val stubbedClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val periodKeys = Seq("23AL", "23AK", "23AD")

  val selectedYear: UserAnswers = emptyUserAnswers.set(CorrectionReturnYearPage(Index(0)), 2023).success.value

  private val monthNames: Seq[Period] = periodKeys.map(ConvertPeriodKey.periodkeyToPeriod)

  private val obligationService: ObligationsService = mock[ObligationsService]

  private val etmpObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AL"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AK"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AD"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AC"
    )
  )

  private val testPeriodsList = Seq(
    Period(2023, Month.JANUARY),
    Period(2023, Month.FEBRUARY),
    Period(2023, Month.MARCH),
    Period(2023, Month.APRIL),
    Period(2023, Month.MAY),
    Period(2023, Month.JUNE),
    Period(2023, Month.JULY),
    Period(2023, Month.AUGUST),
    Period(2023, Month.SEPTEMBER),
    Period(2023, Month.OCTOBER),
    Period(2023, Month.NOVEMBER),
    Period(2023, Month.DECEMBER)
  )
  private val formProvider = new CorrectionReturnPeriodFormProvider()
  private val form: Form[Period] = formProvider(index, testPeriodsList, Seq.empty)

  private lazy val correctionReturnPeriodRoute: String = controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index).url

  "CorrectionReturnPeriod Controller" - {

    "must return OK and the correct view for a GET" - {

      "and must not contain correction years and months older than 3 years from the due date (payment deadline)" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(selectedYear), clock = Some(stubbedClock))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, waypoints, period, monthNames, index)(request, messages(application)).toString
      }
    }
  }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(selectedYear), clock = Some(stubbedClock))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, waypoints, period, monthNames, index)(request, messages(application)).toString
      }

    }


    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), clock = Some(stubbedClock))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", Period(2023, Month.DECEMBER).toString))

        val result = route(application, request).value

        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnPeriodPage(index), Period(2023, Month.DECEMBER)).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionReturnPeriodPage(index).navigate(waypoints, expectedAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(selectedYear), clock = Some(stubbedClock))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionReturnPeriodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm, waypoints, period, monthNames, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnPeriodRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
