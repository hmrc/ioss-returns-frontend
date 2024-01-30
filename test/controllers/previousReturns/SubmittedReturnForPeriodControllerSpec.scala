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
import connectors.{FinancialDataConnector, VatReturnConnector}
import models.UnexpectedResponseStatus
import models.etmp.{EtmpVatReturn, EtmpVatReturnCorrection}
import models.financialdata.Charge
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
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

import java.time.{LocalDate, LocalDateTime}

class SubmittedReturnForPeriodControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val vatReturn = etmpVatReturn
  private val charge: Charge = arbitraryCharge.arbitrary.sample.value

  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]
  private val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockVatReturnConnector)
  }

  "SubmittedReturnForPeriod Controller" - {

    "must return OK and the correct view for a GET" - {

      "when there are corrections present" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturn).toFuture
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturn),
              SubmittedReturnForPeriodSummary.rowAmountPaid(Some(charge.clearedAmount)),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturn)
            ).flatten
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturn),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturn)
              ).flatten
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

          val outstandingAmount: BigDecimal = charge.outstandingAmount
          val vatDeclared = vatReturn.totalVATAmountDueForAllMSGBP

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
              outstandingAmount,
              vatDeclared,
              displayPayNow = vatDeclared > 0 && outstandingAmount > 0
            )(request, messages(application)).toString
        }
      }

      "when there are no corrections present" in {

        val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowAmountPaid(Some(charge.clearedAmount)),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturnNoCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturnNoCorrections)
            ).flatten
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturnNoCorrections),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturnNoCorrections)
              ).flatten
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

          val outstandingAmount: BigDecimal = charge.outstandingAmount
          val vatDeclared = vatReturn.totalVATAmountDueForAllMSGBP

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
              outstandingAmount,
              vatDeclared,
              displayPayNow = vatDeclared > 0 && outstandingAmount > 0
            )(request, messages(application)).toString
        }
      }

      "when there are no negative corrections present" in {

        val vatReturnPositiveCorrections: EtmpVatReturn =
          vatReturn.copy(correctionPreviousVATReturn =
              Seq(EtmpVatReturnCorrection(
                periodKey = arbitraryPeriodKey.arbitrary.sample.value,
                periodFrom = arbitrary[String].sample.value,
                periodTo = arbitrary[String].sample.value,
                msOfConsumption = arbitraryCountry.arbitrary.sample.value.code,
                totalVATAmountCorrectionGBP = BigDecimal(200.56),
                totalVATAmountCorrectionEUR = BigDecimal(200.56)
            )))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturnPositiveCorrections).toFuture
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnPositiveCorrections),
              SubmittedReturnForPeriodSummary.rowAmountPaid(Some(charge.clearedAmount)),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnPositiveCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturnPositiveCorrections),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturnPositiveCorrections)
            ).flatten
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturnPositiveCorrections),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturnPositiveCorrections)
              ).flatten
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.salesToEuNi.title"))))
            )
          )

          val correctionRowsSummaryList = PreviousReturnsCorrectionsSummary.correctionRows(vatReturnPositiveCorrections)

          val negativeAndZeroBalanceCorrectionCountriesSummaryList =
            PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(vatReturnPositiveCorrections)

          val vatOwedSummaryList = SummaryListViewModel(
            rows = PreviousReturnsVatOwedSummary.row(vatReturnPositiveCorrections)
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.vatOwed.titleWithCorrections"))))
            )
          )

          val outstandingAmount: BigDecimal = charge.outstandingAmount
          val vatDeclared = vatReturnPositiveCorrections.totalVATAmountDueForAllMSGBP

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
              outstandingAmount,
              vatDeclared,
              displayPayNow = vatDeclared > 0 && outstandingAmount > 0
            )(request, messages(application)).toString
        }
      }

      "when there is a nil return" in {

        val nilEtmpVatReturn: EtmpVatReturn =
          EtmpVatReturn(
            returnReference = arbitrary[String].sample.value,
            returnVersion = arbitrary[LocalDateTime].sample.value,
            periodKey = arbitraryPeriodKey.arbitrary.sample.value,
            returnPeriodFrom = arbitrary[LocalDate].sample.value,
            returnPeriodTo = arbitrary[LocalDate].sample.value,
            goodsSupplied = Seq.empty,
            totalVATGoodsSuppliedGBP = BigDecimal(0),
            totalVATAmountPayable = BigDecimal(0),
            totalVATAmountPayableAllSpplied = BigDecimal(0),
            correctionPreviousVATReturn = Seq.empty,
            totalVATAmountFromCorrectionGBP = BigDecimal(0),
            balanceOfVATDueForMS = Seq.empty,
            totalVATAmountDueForAllMSGBP = BigDecimal(0),
            paymentReference = arbitrary[String].sample.value
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn Right(nilEtmpVatReturn).toFuture
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(None).toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(nilEtmpVatReturn),
              SubmittedReturnForPeriodSummary.rowAmountPaid(None),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(None),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(nilEtmpVatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(nilEtmpVatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentReference(nilEtmpVatReturn)
            ).flatten
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(nilEtmpVatReturn),
                PreviousReturnsTotalVatOnSalesSummary.row(nilEtmpVatReturn)
              ).flatten
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.salesToEuNi.title"))))
            )
          )

          val correctionRowsSummaryList = PreviousReturnsCorrectionsSummary.correctionRows(nilEtmpVatReturn)

          val negativeAndZeroBalanceCorrectionCountriesSummaryList =
            PreviousReturnsDeclaredVATNoPaymentDueSummary.summaryRowsOfNegativeAndZeroValues(nilEtmpVatReturn)

          val vatOwedSummaryList = SummaryListViewModel(
            rows = PreviousReturnsVatOwedSummary.row(nilEtmpVatReturn)
          ).withCard(
            card = Card(
              title = Some(CardTitle(content = HtmlContent(msgs("submittedReturnForPeriod.vatOwed.title"))))
            )
          )

          val outstandingAmount: BigDecimal = BigDecimal(0)
          val vatDeclared = nilEtmpVatReturn.totalVATAmountDueForAllMSGBP

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
              outstandingAmount,
              vatDeclared,
              displayPayNow = vatDeclared > 0 && outstandingAmount > 0
            )(request, messages(application)).toString
        }
      }

      "when FinancialData API is down" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .build()

        running(application) {
          when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturn).toFuture
          when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "")).toFuture

          implicit val msgs: Messages = messages(application)

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

          val mainSummaryList = SummaryListViewModel(
            rows = Seq(
              SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturn),
              SubmittedReturnForPeriodSummary.rowAmountPaid(None),
              SubmittedReturnForPeriodSummary.rowRemainingAmount(None),
              SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentDueDate(period),
              SubmittedReturnForPeriodSummary.rowReturnReference(vatReturn),
              SubmittedReturnForPeriodSummary.rowPaymentReference(vatReturn)
            ).flatten
          )

          val salesToEuAndNiSummaryList = SummaryListViewModel(
            rows =
              Seq(
                PreviousReturnsTotalNetValueOfSalesSummary.row(vatReturn),
                PreviousReturnsTotalVatOnSalesSummary.row(vatReturn)
              ).flatten
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

          val outstandingAmount: BigDecimal = vatReturn.totalVATAmountPayable
          val vatDeclared = vatReturn.totalVATAmountDueForAllMSGBP

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
              outstandingAmount,
              vatDeclared,
              displayPayNow = vatDeclared > 0 && outstandingAmount > 0
            )(request, messages(application)).toString
        }
      }
    }

    "must redirect to Journey recovery when vat return retrieval fails" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
        .build()

      running(application) {
        when(mockVatReturnConnector.get(any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "")).toFuture
        when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, period).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
