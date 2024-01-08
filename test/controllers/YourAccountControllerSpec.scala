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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import generators.Generators
import models.RegistrationWrapper
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentsService
import viewmodels.PaymentsViewModel
import views.html.YourAccountView

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  "Your Account Controller" - {

    "should display your account view" - {
      "when registration wrapper is present" in {

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapper)
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }

    "must return OK with no cancelYourRequestToLeave link when a trader is not excluded" in {
      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))


      val mockPaymentsService = mock[PaymentsService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .overrides(
          bind[PaymentsService].toInstance(mockPaymentsService)
        )
        .build()


      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
      when(mockPaymentsService.getUnpaidPayments(any())(any(), any())).thenReturn(Future.successful(List.empty))


      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None
        )(request, messages(application)).toString
      }
    }

    "must return OK with cancelYourRequestToLeave link when a trader is excluded" in {
      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val exclusion = EtmpExclusion(NoLongerSupplies, LocalDate.now().plusDays(2), LocalDate.now().minusDays(1), false)
      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(exclusion)))

      val mockPaymentsService = mock[PaymentsService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .overrides(
          bind[PaymentsService].toInstance(mockPaymentsService)
        )
        .build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
      when(mockPaymentsService.getUnpaidPayments(any())(any(), any())).thenReturn(Future.successful(List.empty))

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          Some(appConfig.cancelYourRequestToLeaveUrl)
        )(request, messages(application)).toString
      }
    }
  }
}