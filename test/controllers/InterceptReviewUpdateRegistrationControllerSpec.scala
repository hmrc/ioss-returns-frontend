/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import models.etmp.intermediary.{EtmpClientDetails, EtmpCustomerIdentificationLegacy, IntermediaryRegistrationWrapper}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.StartReturnPage
import play.api.inject
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.intermediary.IntermediaryClientService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.InterceptReviewUpdateRegistrationView

import scala.concurrent.Future

class InterceptReviewUpdateRegistrationControllerSpec extends SpecBase {

  val clientName = "Mr Tufty Tuff"

  "InterceptReviewUpdateRegistration Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockIntermediaryClientService = mock[IntermediaryClientService]

      when(mockIntermediaryClientService.getClientName(eqTo(false), eqTo(None), eqTo(iossNumber))(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[IntermediaryClientService].toInstance(mockIntermediaryClientService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.InterceptReviewUpdateRegistrationController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InterceptReviewUpdateRegistrationView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val changeRegistrationUrl = appConfig.amendRegistrationUrl
        val continueUrl = StartReturnPage(iossNumber, period, appConfig).navigate(waypoints, emptyUserAnswers, emptyUserAnswers).route.url

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(waypoints, changeRegistrationUrl, continueUrl, isIntermediary = false, clientName = None)(request, messages(application)).toString
        verify(mockIntermediaryClientService).getClientName(eqTo(false), eqTo(None), eqTo(iossNumber))(any[HeaderCarrier])
      }
    }

    "must use the NETP registration URL for an intermediary" in {

      val mockIntermediaryClientService = mock[IntermediaryClientService]
      val intermediaryNumber = "IN9001234567"

      when(mockIntermediaryClientService.getClientName(eqTo(true), eqTo(Some(intermediaryNumber)), eqTo(iossNumber))(any[HeaderCarrier])).thenReturn(Future.successful(Some(clientName)))

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        maybeIntermediaryNumber = Some(intermediaryNumber)
      )
        .overrides(bind[IntermediaryClientService].toInstance(mockIntermediaryClientService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.InterceptReviewUpdateRegistrationController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InterceptReviewUpdateRegistrationView]

        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        val changeRegistrationUrl = s"${appConfig.changeNetpRegistrationUrl}/$iossNumber"

        val continueUrl = StartReturnPage(iossNumber, period, appConfig).navigate(waypoints, emptyUserAnswers, emptyUserAnswers).route.url

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          waypoints,
          changeRegistrationUrl,
          continueUrl,
          isIntermediary = true,
          clientName = Some(clientName)
        )(request, messages(application)).toString

        verify(mockIntermediaryClientService).getClientName(eqTo(true), eqTo(Some(intermediaryNumber)), eqTo(iossNumber))(any[HeaderCarrier])
      }
    }

    "must redirect to Journey Recovery when no answers are found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.InterceptReviewUpdateRegistrationController.onPageLoad(waypoints, iossNumber).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  def intermediaryRegistrationWithClients(iossNumber: Seq[String]): IntermediaryRegistrationWrapper = {
    arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value.copy(
      etmpDisplayRegistration = arbitraryEtmpIntermediaryDisplayRegistration.arbitrary.sample.value.copy(
        customerIdentification = EtmpCustomerIdentificationLegacy(vrn),
        clientDetails = iossNumber.map { ioss =>
          arbitraryEtmpClientDetails.arbitrary.sample.value.copy(clientIossID = ioss)
        }
      )
    )
  }
}
