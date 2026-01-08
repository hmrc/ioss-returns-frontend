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

package controllers.submissionResults

import base.SpecBase
import config.FrontendAppConfig
import connectors.VatReturnConnector
import models.external.ExternalEntryUrl
import models.responses.NotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.TotalAmountVatDueGBPQuery
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.submissionResults.SuccessfullySubmittedView

class SuccessfullySubmittedControllerSpec extends SpecBase with TableDrivenPropertyChecks {
  private val options = Table(
    ("SoldGoodsPage", "CorrectPreviousReturnPage", "nilReturn"),
    (false, false, true),
    (true, false, false),
    (false, true, false),
    (true, true, false)
  )

  private val totalOwed = BigDecimal("200.52")
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]
  private val mockSessionRepository: SessionRepository = mock[SessionRepository]

  "SuccessfullySubmitted Controller" - {

    "must return OK and a view and clear the session with no external url when the saved external entry fails retrieval" in {

      forAll(options) { (soldGoodsPage, correctPreviousReturnPage, nilReturn) =>
        val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"
        val application = createApplication(soldGoodsPage, correctPreviousReturnPage)

        reset(mockVatReturnConnector, mockSessionRepository)
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Left(NotFound).toFuture
        when(mockSessionRepository.clear(any())) thenReturn true.toFuture

        running(application) {
          val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)
          val config = application.injector.instanceOf[FrontendAppConfig]
          val result = route(application, request).value
          val view = application.injector.instanceOf[SuccessfullySubmittedView]

          status(result) mustEqual OK
          val totalOwed = BigDecimal(200.52)

          contentAsString(result) mustEqual view(
            returnReference = returnReference,
            nilReturn = nilReturn,
            period = period,
            owedAmount = totalOwed,
            externalUrl = None,
            "https://test-url.com",
            isIntermediary = false,
            clientName = "Mr Tufftys Tuffs",
            intermediaryDashboardUrl = config.intermediaryDashboardUrl
          )(request, messages(application)).toString
          verify(mockSessionRepository, times(1)).clear(any())
        }
      }
    }

    def createApplication(soldGoodsPage: Boolean, correctPreviousReturnPage: Boolean) = {
      val completedAnswers = completeUserAnswers
        .set(TotalAmountVatDueGBPQuery, totalOwed).success.value
        .set(SoldGoodsPage, soldGoodsPage).success.value
        .set(CorrectPreviousReturnPage(0), correctPreviousReturnPage)
        .success.value

      applicationBuilder(userAnswers = Some(completedAnswers))
        .configure("urls.userResearch2" -> "https://test-url.com")
        .overrides(
          bind[VatReturnConnector].toInstance(mockVatReturnConnector),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()
    }

    "must return OK and the correct view and clear the session for a GET with an external url passed if one can be retrieved" in {

      forAll(options) { (soldGoodsPage, correctPreviousReturnPage, nilReturn) =>

        val externalUrlOptions = Table(
          "maybe external url",
          Some("external-url"),
          None
        )

        forAll(externalUrlOptions) { maybeExternalUrl =>
          val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"
          val application = createApplication(soldGoodsPage, correctPreviousReturnPage)

          reset(mockVatReturnConnector, mockSessionRepository)
          when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(maybeExternalUrl)).toFuture
          when(mockSessionRepository.clear(any())) thenReturn true.toFuture

          running(application) {
            val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)
            val config = application.injector.instanceOf[FrontendAppConfig]
            val result = route(application, request).value
            val view = application.injector.instanceOf[SuccessfullySubmittedView]

            status(result) mustEqual OK
            val totalOwed = BigDecimal(200.52)

            contentAsString(result) mustEqual view(
              returnReference = returnReference,
              nilReturn = nilReturn,
              period = period,
              owedAmount = totalOwed,
              externalUrl = maybeExternalUrl,
              "https://test-url.com",
              isIntermediary = false,
              clientName = "Mr Tufftys Tuffs",
              intermediaryDashboardUrl = config.intermediaryDashboardUrl
            )(request, messages(application)).toString
            verify(mockSessionRepository, times(1)).clear(any())
          }
        }
      }
    }

    "must return OK and a view and clear the session with no external url when (SoldGoodsPage, None) => nilReturn = true" in {

      val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"
      val application = createApplication(soldGoodsPage = false, correctPreviousReturnPage = false)

      reset(mockVatReturnConnector, mockSessionRepository)
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Left(NotFound).toFuture
      when(mockSessionRepository.clear(any())) thenReturn true.toFuture

      running(application) {
        val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)
        val config = application.injector.instanceOf[FrontendAppConfig]
        val result = route(application, request).value
        val view = application.injector.instanceOf[SuccessfullySubmittedView]

        status(result) mustEqual OK
        val totalOwed = BigDecimal(200.52)

        contentAsString(result) mustEqual view(
          returnReference = returnReference,
          nilReturn = true,
          period = period,
          owedAmount = totalOwed,
          externalUrl = None,
          "https://test-url.com",
          isIntermediary = false,
          clientName = "Mr Tufftys Tuffs",
          intermediaryDashboardUrl = config.intermediaryDashboardUrl
        )(request, messages(application)).toString
        verify(mockSessionRepository, times(1)).clear(any())
      }
    }

    "must throw RuntimeException when TotalAmountVatDueGBPQuery is missing in userAnswers" in {

      val incompleteAnswers = completeUserAnswers
        .set(SoldGoodsPage, false).success.value
        .set(CorrectPreviousReturnPage(0), false).success.value

      reset(mockSessionRepository)

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)

        val exception = intercept[RuntimeException] {
          val result = route(application, request).value
          contentAsString(result) // Force evaluation
        }

        exception.getMessage mustEqual "TotalAmountVatDueGBPQuery has not been set in answers"
        verifyNoInteractions(mockSessionRepository)
      }
    }
  }
}
