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
import forms.corrections.CorrectionReturnYearFormProvider
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import models.Index
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectionReturnYearPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ObligationsService
import utils.FutureSyntax.FutureOps
import views.html.corrections.CorrectionReturnYearView

import scala.concurrent.Future

class CorrectionReturnYearControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val periodKeys = Seq("23AL", "23AK" , "22AK")

  private val periodYears: Seq[Int] = periodKeys.map { periodYear =>
    s"20${periodYear.substring(0, 2)}".toInt
  }

  private val distinctPeriodYears = periodYears.distinct

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
      periodKey = "22AK"
    )
  )

  private val formProvider = new CorrectionReturnYearFormProvider()
  private val form: Form[Int] = formProvider(index, Seq.empty)

  lazy val correctionReturnYearRoute: String = controllers.corrections.routes.CorrectionReturnYearController.onPageLoad(waypoints, index).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(obligationService)
  }

  "CorrectionReturnYear Controller" - {

    "must return OK and the correct view for a GET" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnYearRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
            form, waypoints, period, utils.ItemsHelper.radioButtonItems(distinctPeriodYears), index)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(obligationService.getFulfilledObligations(any())(any())).thenReturn(Future.successful(etmpObligationDetails))

      val userAnswers = emptyUserAnswers.set(CorrectionReturnYearPage(Index(0)), 2021).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionReturnYearRoute)

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form, waypoints, period, utils.ItemsHelper.radioButtonItems(distinctPeriodYears), index)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      val updatedAnswers = emptyUserAnswers.set(CorrectionReturnYearPage(index), 2023).success.value

      when(mockSessionRepository.set(updatedAnswers)) thenReturn Future.successful(true)
      when(obligationService.getFulfilledObligations(any())(any()))
        .thenReturn(Future.successful(Seq(
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Open,
            periodKey = "23AL"
          )
        )))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnYearRoute)
            .withFormUrlEncodedBody(("value", 2023.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.CorrectionReturnPeriodController.onPageLoad(waypoints, index).url
        verify(mockSessionRepository, times(1)).set(eqTo(updatedAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())).thenReturn(Future.successful(etmpObligationDetails))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, correctionReturnYearRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionReturnYearView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm, waypoints, period, utils.ItemsHelper.radioButtonItems(distinctPeriodYears), index)(request, messages(application)).toString
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
