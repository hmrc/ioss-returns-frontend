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

package controllers.submissionResults

import base.SpecBase
import connectors.VatReturnConnector
import models.NotFound
import models.external.ExternalEntryUrl
import org.mockito.ArgumentMatchers.any
import org.mockito.IdiomaticMockito
import org.mockito.Mockito.when
import org.scalatest.prop.TableDrivenPropertyChecks
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.TotalAmountVatDueGBPQuery
import views.html.submissionResults.SuccessfullySubmittedView

import scala.concurrent.Future


class SuccessfullySubmittedControllerSpec extends SpecBase with TableDrivenPropertyChecks with IdiomaticMockito {
  private val options = Table(
    ("SoldGoodsPage", "CorrectPreviousReturnPage", "nilReturn"),
    (false, false, true),
    (true, false, false),
    (false, true, false),
    (true, true, false)
  )

  private val totalOwed = BigDecimal("200.52")
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  "SuccessfullySubmitted Controller" - {

    "must return OK and a view with no external url when the saved external entry fails retrieval" in {
      forAll(options) { (soldGoodsPage, correctPreviousReturnPage, nilReturn) =>
        val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"
        val application = createApplication(soldGoodsPage, correctPreviousReturnPage)

        reset(mockVatReturnConnector)
        when(mockVatReturnConnector.getSavedExternalEntry()(any()))
          .thenReturn(Future.successful(Left(NotFound)))

        running(application) {
          val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[SuccessfullySubmittedView]

          status(result) mustEqual OK
          val totalOwed = BigDecimal(200.52)

          contentAsString(result) mustEqual view(
            returnReference = returnReference,
            nilReturn = nilReturn,
            period = period,
            owedAmount = totalOwed,
            externalUrl = None
          )(request, messages(application)).toString
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
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()
    }

    "must return OK and the correct view for a GET with an external url passed if one can be retrieved" in {
      forAll(options) { (soldGoodsPage, correctPreviousReturnPage, nilReturn) =>

        val externalUrlOptions = Table(
          "maybe external url",
          Some("external-url"),
          None
        )

        forAll(externalUrlOptions) { maybeExternalUrl =>
          val returnReference = s"XI/${iossNumber}/M0${period.month.getValue}.${period.year}"
          val application = createApplication(soldGoodsPage, correctPreviousReturnPage)

          reset(mockVatReturnConnector)
          when(mockVatReturnConnector.getSavedExternalEntry()(any()))
            .thenReturn(Future.successful(Right(ExternalEntryUrl(maybeExternalUrl))))

          running(application) {
            val request = FakeRequest(GET, routes.SuccessfullySubmittedController.onPageLoad().url)
            val result = route(application, request).value
            val view = application.injector.instanceOf[SuccessfullySubmittedView]

            status(result) mustEqual OK
            val totalOwed = BigDecimal(200.52)

            contentAsString(result) mustEqual view(
              returnReference = returnReference,
              nilReturn = nilReturn,
              period = period,
              owedAmount = totalOwed,
              externalUrl = maybeExternalUrl
            )(request, messages(application)).toString
          }
        }
      }
    }
  }
}
