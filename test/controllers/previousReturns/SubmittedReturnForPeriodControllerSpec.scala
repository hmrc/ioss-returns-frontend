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
import connectors.VatReturnConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testUtils.EtmpVatReturnData.etmpVatReturn
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle}
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist._
import viewmodels.previousReturns._
import views.html.previousReturns.SubmittedReturnForPeriodView

class SubmittedReturnForPeriodControllerSpec extends SpecBase {

  private val vatReturn = etmpVatReturn

  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  "SubmittedReturnForPeriod Controller" - {

    "must return OK and the correct view for a GET" - {

      "when there are corrections present" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn vatReturn.toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturn),
              SubmittedReturnForPeriodSummary.rowAmountPaid(),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturn)
            )
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturn),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturn)
              )
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.salesToEuNi.title"))))
            )
          )

          val correctionRowsSummaryList = PreviousReturnsCorrectionsSummary.correctionRows(vatReturn)

          val negativeAndZeroBalanceCorrectionCountriesSummaryList =
            PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(vatReturn)

          val vatOwedSummaryList = SummaryListViewModel(
            rows = PreviousReturnsVatOwedSummary.row(vatReturn)
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.vatOwed.titleWithCorrections"))))
            )
          )

          val totalVatPayable = vatReturn.totalVATAmountDueForAllMSGBP

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(
              waypoints,
              period,
              mainSummaryList,
              salesToEuAndNiSummaryList,
              correctionRowsSummaryList,
              negativeAndZeroBalanceCorrectionCountriesSummaryList,
              vatOwedSummaryList,
              totalVatPayable
            )(request, messages(application)).toString
        }
      }

      "when there are not corrections present" in {

        val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn vatReturnNoCorrections.toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowAmountPaid(),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturnNoCorrections)
            )
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturnNoCorrections),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturnNoCorrections)
              )
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.salesToEuNi.title"))))
            )
          )

          val correctionRowsSummaryList = PreviousReturnsCorrectionsSummary.correctionRows(vatReturnNoCorrections)

          val negativeAndZeroBalanceCorrectionCountriesSummaryList =
            PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(vatReturnNoCorrections)

          val vatOwedSummaryList = SummaryListViewModel(
            rows = PreviousReturnsVatOwedSummary.row(vatReturnNoCorrections)
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.vatOwed.title"))))
            )
          )

          val totalVatPayable = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(
              waypoints,
              period,
              mainSummaryList,
              salesToEuAndNiSummaryList,
              correctionRowsSummaryList,
              negativeAndZeroBalanceCorrectionCountriesSummaryList,
              vatOwedSummaryList,
              totalVatPayable
            )(request, messages(application)).toString
        }
      }
    }
  }
}
