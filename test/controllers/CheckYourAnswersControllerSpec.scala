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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.CorrectPreviousReturnPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{CoreVatReturnService, SalesAtVatRateService}
import viewmodels.govuk.SummaryListFluency

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val salesAtVatRateService = mock[SalesAtVatRateService]
  private val mockCoreVatReturnService = mock[CoreVatReturnService]

  override def beforeEach(): Unit = {
    Mockito.reset(salesAtVatRateService)
    Mockito.reset(mockCoreVatReturnService)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {
    "onPageLoad" - {
      "when correct previous return is false / empty" - {
        "must return OK and the correct view for a GET" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage, false).success.value))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result).contains("Business name") mustBe true
            contentAsString(result).contains("UK VAT registration number") mustBe true
            contentAsString(result).contains("Return month") mustBe true
            contentAsString(result).contains("Sales to EU countries and Northern Ireland") mustBe true
            contentAsString(result).contains("Sales made") mustBe true
            contentAsString(result).contains("Sales excluding VAT") mustBe true
            contentAsString(result).contains("Corrections") mustBe true
            //          contentAsString(result).contains("Corrections made") mustBe true
            contentAsString(result).contains("VAT owed") mustBe true
            contentAsString(result).contains("Total VAT payable") mustBe true
          }
        }

        "must return OK and the correct view for a GET when the correction choice was NO " in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage, false).success.value))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result).contains("Business name") mustBe true
            contentAsString(result).contains("UK VAT registration number") mustBe true
            contentAsString(result).contains("Return month") mustBe true
            contentAsString(result).contains("Sales to EU countries and Northern Ireland") mustBe true
            contentAsString(result).contains("Sales made") mustBe true
            contentAsString(result).contains("Sales excluding VAT") mustBe true
            contentAsString(result).contains("Corrections") mustBe true
            //          contentAsString(result).contains("Corrections made") mustBe true
            contentAsString(result).contains("VAT owed") mustBe true
            contentAsString(result).contains("Total VAT payable") mustBe true
          }
        }

      }


      "when correct previous return is true" - {

        "must contain VAT declared to EU countries after corrections heading if there were corrections and all totals are positive" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage, true).success.value))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result).contains("Business name") mustBe true
            contentAsString(result).contains("UK VAT registration number") mustBe true
            contentAsString(result).contains("Return month") mustBe true
            contentAsString(result).contains("Sales to EU countries and Northern Ireland") mustBe true
            contentAsString(result).contains("Sales made") mustBe true
            contentAsString(result).contains("Sales excluding VAT") mustBe true
            contentAsString(result).contains("Corrections") mustBe true
            //          contentAsString(result).contains("Corrections made") mustBe true
            contentAsString(result).contains("VAT owed") mustBe true
            contentAsString(result).contains("Total VAT payable") mustBe true
          }
        }

        "must contain VAT declared where no payment is due heading if there were negative totals after corrections" in {

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage, true).success.value))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result).contains("Business name") mustBe true
            contentAsString(result).contains("UK VAT registration number") mustBe true
            contentAsString(result).contains("Return month") mustBe true
            contentAsString(result).contains("Sales to EU countries and Northern Ireland") mustBe true
            contentAsString(result).contains("Sales made") mustBe true
            contentAsString(result).contains("Sales excluding VAT") mustBe true
            contentAsString(result).contains("Corrections") mustBe true
            //          contentAsString(result).contains("Corrections made") mustBe true
            contentAsString(result).contains("VAT owed") mustBe true
            contentAsString(result).contains("Total VAT payable") mustBe true
          }
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "when the user answered all necessary data and submission of the return succeeds" in {

        when(mockCoreVatReturnService.submitCoreVatReturn(any())(any())) thenReturn
          Future.successful(true)

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[CoreVatReturnService].toInstance(mockCoreVatReturnService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = false).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.submissionResults.routes.SuccessfullySubmittedController.onPageLoad().url
        }
      }

      "when the user answered all necessary data and submission of the return fails" in {

        when(mockCoreVatReturnService.submitCoreVatReturn(any())(any())) thenReturn
          Future.successful(false)

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(bind[CoreVatReturnService].toInstance(mockCoreVatReturnService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = false).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad().url
        }
      }
    }

  }
}
