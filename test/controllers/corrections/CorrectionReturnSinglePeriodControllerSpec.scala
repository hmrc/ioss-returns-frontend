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
import forms.corrections.CorrectionReturnSinglePeriodFormProvider
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnSinglePeriodPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ObligationsService
import utils.ConvertPeriodKey
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionReturnSinglePeriodView

import scala.concurrent.Future

class CorrectionReturnSinglePeriodControllerSpec extends SpecBase with MockitoSugar {


  private val periodKeys = Seq("23AK")

  private val periodYear: Seq[Int] = periodKeys.map { periodYear =>
    s"20${periodYear.substring(0, 2)}".toInt
  }

  private val monthName: Seq[String] = periodKeys.map(ConvertPeriodKey.monthNameFromEtmpPeriodKey)

  private val monthAndYear = s"${monthName.mkString(", ")} ${periodYear.mkString(", ")}"
  private val obligationService: ObligationsService = mock[ObligationsService]

  private val etmpObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Open,
      periodKey = "23AK"
    )
  )
  val formProvider = new CorrectionReturnSinglePeriodFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val correctionReturnSinglePeriodRoute: String = controllers.corrections.routes.CorrectionReturnSinglePeriodController.onPageLoad(waypoints, index).url

  "CorrectionReturnSinglePeriod Controller" - {

    "must return OK and the correct view for a GET" in {

      when(obligationService.getOpenObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnSinglePeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form,waypoints, period, monthAndYear, index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(obligationService.getOpenObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val userAnswers = emptyUserAnswers.set(CorrectionReturnSinglePeriodPage(index), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)

        val view = application.injector.instanceOf[CorrectionReturnSinglePeriodView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), waypoints, period, monthAndYear, index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(obligationService.getOpenObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionReturnSinglePeriodPage(index), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionReturnSinglePeriodPage(index).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(obligationService.getOpenObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionReturnSinglePeriodView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, waypoints, period, monthAndYear, index)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnSinglePeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnSinglePeriodRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
