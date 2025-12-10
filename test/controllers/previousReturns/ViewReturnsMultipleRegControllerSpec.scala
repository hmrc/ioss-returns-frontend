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

package controllers.previousReturns

import base.SpecBase
import models.StandardPeriod
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SelectedPreviousRegistrationRepository
import services.{ObligationsService, PaymentsService, PreviousRegistrationService}
import testUtils.PeriodWithFinancialData.{obligationDetails, periodsWithFinancialData, prepareData}
import testUtils.PreviousRegistrationData.{previousRegistrations, selectedPreviousRegistration}
import utils.FutureSyntax.FutureOps
import viewmodels.previousReturns._
import views.html.previousReturns.ViewReturnsMultipleRegView

import java.time.YearMonth

class ViewReturnsMultipleRegControllerSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockSelectedPreviousRegistrationRepository: SelectedPreviousRegistrationRepository = mock[SelectedPreviousRegistrationRepository]
  private val mockPaymentsService: PaymentsService = mock[PaymentsService]
  private val mockObligationsService: ObligationsService = mock[ObligationsService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(mockSelectedPreviousRegistrationRepository)
    Mockito.reset(mockPaymentsService)
    Mockito.reset(mockObligationsService)
  }

  "ReturnRegistrationSelection Controller" - {

    "must return OK and the correct view for a GET " +
      "when there are multiple previous registrations and the selected registration is part of the previous registrations" in {

      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[SelectedPreviousRegistrationRepository].toInstance(mockSelectedPreviousRegistrationRepository))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockSelectedPreviousRegistrationRepository.get(userAnswersId)) thenReturn Some(selectedPreviousRegistration).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture

        val request = FakeRequest(GET, routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewReturnsMultipleRegView]

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(waypoints, selectedPreviousRegistration.previousRegistration, periodsWithFinancialData)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecoveryPage " +
      "when there are multiple previous registrations and the selected registration is not part of the previous registrations" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[SelectedPreviousRegistrationRepository].toInstance(mockSelectedPreviousRegistrationRepository))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

        val selectedPreviousRegistration = SelectedPreviousRegistration(userAnswersId, PreviousRegistration(
          "IM900111111111",
          StandardPeriod(YearMonth.of(2020, 1)),
          StandardPeriod(YearMonth.of(2022, 9))
        ))
        when(mockSelectedPreviousRegistrationRepository.get(userAnswersId)) thenReturn Some(selectedPreviousRegistration).toFuture

        val request = FakeRequest(GET, routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to JourneyRecoveryPage " +
      "when there are multiple previous registrations and there is no selected registration" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[SelectedPreviousRegistrationRepository].toInstance(mockSelectedPreviousRegistrationRepository))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockSelectedPreviousRegistrationRepository.get(userAnswersId)) thenReturn None.toFuture

        val request = FakeRequest(GET, routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must return OK and the correct view for a GET when there is a single previous registration" in {

      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.take(1).toFuture
        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture

        val request = FakeRequest(GET, routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewReturnsMultipleRegView]

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(waypoints, previousRegistrations.head, periodsWithFinancialData)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecoveryPage when there are no previous registrations" in {
      val application = applicationBuilder()
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn List.empty.toFuture

        val request = FakeRequest(GET, routes.ViewReturnsMultipleRegController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
