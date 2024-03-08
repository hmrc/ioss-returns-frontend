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
import models.etmp.{EtmpExclusion, EtmpExclusionReason, EtmpVatReturn, EtmpVatReturnCorrection}
import models.financialdata.Charge
import models.requests.OptionalDataRequest
import models.{Period, RegistrationWrapper, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.JourneyRecoveryPage
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PreviousRegistrationService
import testUtils.EtmpVatReturnData.etmpVatReturn
import testUtils.PreviousRegistrationData.previousRegistrations
import testUtils.RegistrationData.etmpDisplayRegistration
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle}
import utils.FutureSyntax.FutureOps
import viewmodels.govuk.summarylist._
import viewmodels.previousReturns._
import views.html.previousReturns.SubmittedReturnForPeriodView

import java.time.{LocalDate, LocalDateTime}

class SubmittedReturnForPeriodControllerSpec extends SpecBase with BeforeAndAfterEach with PrivateMethodTester {

  private val vatReturn = etmpVatReturn
  private val charge: Charge = arbitraryCharge.arbitrary.sample.value

  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]
  private val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]
  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockVatReturnConnector)
    Mockito.reset(mockFinancialDataConnector)
    Mockito.reset(mockPreviousRegistrationService)
  }

  "SubmittedReturnForPeriod Controller" - {

    "onPageLoad" - {
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
              )(request, messages(application)).toString
          }
        }

        "when there are exclusions present and vat return due date has exceeded 3 years" - {
          "and nothing owed" in {

            val charge = Charge(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(0),
              clearedAmount = BigDecimal(1000)
            )

            val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
            val exceededDate = date.minusYears(3).minusMonths(2)
            val exceededPeriod = Period(exceededDate.getYear, exceededDate.getMonth)
            val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty, periodKey = exceededPeriod.toEtmpPeriodString)

            val etmpExclusion: EtmpExclusion = EtmpExclusion(
              exclusionReason = EtmpExclusionReason.NoLongerSupplies,
              effectiveDate = LocalDate.now(stubClockAtArbitraryDate).plusMonths(6),
              decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusMonths(7),
              quarantine = false
            )

            val registrationWrapper = RegistrationWrapper(
              vatInfo = arbitraryVatInfo.arbitrary.sample.value,
              registration = etmpDisplayRegistration.copy(exclusions = Seq(etmpExclusion))
            )

            val application = applicationBuilder(userAnswers = None, registration = registrationWrapper)
              .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
              .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
              .build()

            running(application) {
              when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

              implicit val msgs: Messages = messages(application)

              val request =
                OptionalDataRequest(
                  FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, exceededPeriod).url),
                  testCredentials,
                  vrn,
                  iossNumber,
                  registrationWrapper,
                  None
                )

              val result = route(application, request).value

              val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
                  SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowPaymentDueDate(exceededPeriod),
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
              val vatDeclared = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  waypoints,
                  exceededPeriod,
                  mainSummaryList,
                  salesToEuAndNiSummaryList,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  outstandingAmount,
                  vatDeclared,
                  displayPayNow = false,
                  returnIsExcludedAndOutstandingAmount = false
                )(request, messages(application)).toString
            }
          }

          "and something owed" in {

            val charge = Charge(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(500),
              clearedAmount = BigDecimal(500)
            )

            val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
            val exceededDate = date.minusYears(3).minusMonths(2)
            val exceededPeriod = Period(exceededDate.getYear, exceededDate.getMonth)
            val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty, periodKey = exceededPeriod.toEtmpPeriodString)

            val etmpExclusion: EtmpExclusion = EtmpExclusion(
              exclusionReason = EtmpExclusionReason.NoLongerSupplies,
              effectiveDate = LocalDate.now(stubClockAtArbitraryDate).plusMonths(6),
              decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusMonths(7),
              quarantine = false
            )

            val registrationWrapper = RegistrationWrapper(
              vatInfo = arbitraryVatInfo.arbitrary.sample.value,
              registration = etmpDisplayRegistration.copy(exclusions = Seq(etmpExclusion))
            )

            val application = applicationBuilder(userAnswers = None, registration = registrationWrapper)
              .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
              .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
              .build()

            running(application) {
              when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getCharge(any())(any())) thenReturn Right(Some(charge)).toFuture

              implicit val msgs: Messages = messages(application)

              val request =
                OptionalDataRequest(
                  FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, exceededPeriod).url),
                  testCredentials,
                  vrn,
                  iossNumber,
                  registrationWrapper,
                  None
                )

              val result = route(application, request).value

              val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
                  SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowPaymentDueDate(exceededPeriod),
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
              val vatDeclared = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  waypoints,
                  exceededPeriod,
                  mainSummaryList,
                  salesToEuAndNiSummaryList,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  outstandingAmount,
                  vatDeclared,
                  displayPayNow = false,
                  returnIsExcludedAndOutstandingAmount = true
                )(request, messages(application)).toString
            }
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

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
        }
      }
    }


    "onPageLoadForIossNumber" - {
      "must return OK and the correct view for a GET" - {

        "when there are corrections present" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
            .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
            .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
            .build()

          running(application) {
            when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturn).toFuture
            when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturn),
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
              )(request, messages(application)).toString
          }
        }

        "when there are no corrections present" in {

          val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty)

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
            .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
            .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
            .build()

          running(application) {
            when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
            when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
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
            val vatDeclared = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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
            .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
            .build()

          running(application) {
            when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturnPositiveCorrections).toFuture
            when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
            when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnPositiveCorrections),
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
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
            .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
            .build()

          running(application) {
            when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(nilEtmpVatReturn).toFuture
            when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(None).toFuture
            when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                SubmittedReturnForPeriodSummary.rowVatDeclared(nilEtmpVatReturn),
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
              )(request, messages(application)).toString
          }
        }

        "when FinancialData API is down" in {

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
            .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
            .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
            .build()

          running(application) {
            when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturn).toFuture
            when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn
              Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "")).toFuture
            when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

            val mainSummaryList = SummaryListViewModel(
              rows = Seq(
                SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturn),
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

            status(result) mustBe OK
            contentAsString(result) mustBe
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
                displayPayNow = vatDeclared > 0 && outstandingAmount > 0,
                returnIsExcludedAndOutstandingAmount = false
              )(request, messages(application)).toString
          }
        }

        "when there are exclusions present and vat return due date has exceeded 3 years" - {
          "and nothing owed" in {

            val charge = Charge(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(0),
              clearedAmount = BigDecimal(1000)
            )

            val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
            val exceededDate = date.minusYears(3).minusMonths(2)
            val exceededPeriod = Period(exceededDate.getYear, exceededDate.getMonth)
            val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty, periodKey = exceededPeriod.toEtmpPeriodString)

            val etmpExclusion: EtmpExclusion = EtmpExclusion(
              exclusionReason = EtmpExclusionReason.NoLongerSupplies,
              effectiveDate = LocalDate.now(stubClockAtArbitraryDate).plusMonths(6),
              decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusMonths(7),
              quarantine = false
            )

            val registrationWrapper = RegistrationWrapper(
              vatInfo = arbitraryVatInfo.arbitrary.sample.value,
              registration = etmpDisplayRegistration.copy(exclusions = Seq(etmpExclusion))
            )

            val application = applicationBuilder(userAnswers = None, registration = registrationWrapper)
              .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
              .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
              .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
              .build()

            running(application) {
              when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
              when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

              implicit val msgs: Messages = messages(application)

              val request =
                OptionalDataRequest(
                  FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, exceededPeriod, iossNumber).url),
                  testCredentials,
                  vrn,
                  iossNumber,
                  registrationWrapper,
                  None
                )

              val result = route(application, request).value

              val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
                  SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowPaymentDueDate(exceededPeriod),
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
              val vatDeclared = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  waypoints,
                  exceededPeriod,
                  mainSummaryList,
                  salesToEuAndNiSummaryList,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  outstandingAmount,
                  vatDeclared,
                  displayPayNow = false,
                  returnIsExcludedAndOutstandingAmount = false
                )(request, messages(application)).toString
            }
          }

          "and something owed" in {

            val charge = Charge(
              period = period,
              originalAmount = BigDecimal(1000),
              outstandingAmount = BigDecimal(500),
              clearedAmount = BigDecimal(500)
            )

            val date = LocalDate.ofInstant(stubClockAtArbitraryDate.instant(), stubClockAtArbitraryDate.getZone)
            val exceededDate = date.minusYears(3).minusMonths(2)
            val exceededPeriod = Period(exceededDate.getYear, exceededDate.getMonth)
            val vatReturnNoCorrections = vatReturn.copy(correctionPreviousVATReturn = Seq.empty, periodKey = exceededPeriod.toEtmpPeriodString)

            val etmpExclusion: EtmpExclusion = EtmpExclusion(
              exclusionReason = EtmpExclusionReason.NoLongerSupplies,
              effectiveDate = LocalDate.now(stubClockAtArbitraryDate).plusMonths(6),
              decisionDate = LocalDate.now(stubClockAtArbitraryDate).minusMonths(7),
              quarantine = false
            )

            val registrationWrapper = RegistrationWrapper(
              vatInfo = arbitraryVatInfo.arbitrary.sample.value,
              registration = etmpDisplayRegistration.copy(exclusions = Seq(etmpExclusion))
            )

            val application = applicationBuilder(userAnswers = None, registration = registrationWrapper)
              .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
              .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
              .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
              .build()

            running(application) {
              when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(vatReturnNoCorrections).toFuture
              when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
              when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

              implicit val msgs: Messages = messages(application)

              val request =
                OptionalDataRequest(
                  FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, exceededPeriod, iossNumber).url),
                  testCredentials,
                  vrn,
                  iossNumber,
                  registrationWrapper,
                  None
                )

              val result = route(application, request).value

              val view = application.injector.instanceOf[SubmittedReturnForPeriodView]

              val mainSummaryList = SummaryListViewModel(
                rows = Seq(
                  SubmittedReturnForPeriodSummary.rowVatDeclared(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowRemainingAmount(Some(charge.outstandingAmount)),
                  SubmittedReturnForPeriodSummary.rowReturnSubmittedDate(vatReturnNoCorrections),
                  SubmittedReturnForPeriodSummary.rowPaymentDueDate(exceededPeriod),
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
              val vatDeclared = vatReturnNoCorrections.totalVATAmountDueForAllMSGBP

              status(result) mustBe OK
              contentAsString(result) mustBe
                view(
                  waypoints,
                  exceededPeriod,
                  mainSummaryList,
                  salesToEuAndNiSummaryList,
                  correctionRowsSummaryList,
                  negativeAndZeroBalanceCorrectionCountriesSummaryList,
                  vatOwedSummaryList,
                  outstandingAmount,
                  vatDeclared,
                  displayPayNow = false,
                  returnIsExcludedAndOutstandingAmount = true
                )(request, messages(application)).toString
            }
          }
        }
      }

      "must redirect to Journey recovery when vat return retrieval fails" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .overrides(bind[FinancialDataConnector].toInstance(mockFinancialDataConnector))
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

        running(application) {
          when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "")).toFuture
          when(mockFinancialDataConnector.getChargeForIossNumber(any(), any())(any())) thenReturn Right(Some(charge)).toFuture
          when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, iossNumber).url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
        }
      }

      "must redirect to Journey recovery when IOSS number is not part of previous registrations or request.iossNumber" in {

        val application = applicationBuilder()
          .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
          .build()

        running(application) {
          when(mockPreviousRegistrationService.getPreviousRegistrations()(any())) thenReturn previousRegistrations.toFuture

          val request = FakeRequest(GET, routes.SubmittedReturnForPeriodController.onPageLoadForIossNumber(waypoints, period, "IM9001111111").url)

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
        }
      }
    }

    ".hasActiveReturnWindowExpired" - {

      "must return true if active return window has expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3).minusDays(1)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val controller = application.injector.instanceOf[SubmittedReturnForPeriodController]

          val privateMethodCall = PrivateMethod[Boolean](Symbol("hasActiveWindowExpired"))
          val result = controller invokePrivate privateMethodCall(dueDate)

          result mustBe true
        }
      }

      "must return false if active return window is on the day of expiry" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(3)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val controller = application.injector.instanceOf[SubmittedReturnForPeriodController]

          val privateMethodCall = PrivateMethod[Boolean](Symbol("hasActiveWindowExpired"))
          val result = controller invokePrivate privateMethodCall(dueDate)

          result mustBe false
        }
      }

      "must return false if active return window has not expired" in {

        val dueDate: LocalDate = LocalDate.now(stubClockAtArbitraryDate).minusYears(2)

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val controller = application.injector.instanceOf[SubmittedReturnForPeriodController]

          val privateMethodCall = PrivateMethod[Boolean](Symbol("hasActiveWindowExpired"))
          val result = controller invokePrivate privateMethodCall(dueDate)

          result mustBe false
        }
      }
    }

    ".isCurrentlyExcluded" - {

      "must return false if there is an exclusion that is a reversal" in {

        val etmpExclusionWithReversal: Seq[EtmpExclusion] = Seq(arbitraryEtmpExclusion.arbitrary.sample.value
          .copy(exclusionReason = EtmpExclusionReason.Reversal))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val controller = application.injector.instanceOf[SubmittedReturnForPeriodController]

          val privateMethodCall = PrivateMethod[Boolean](Symbol("isCurrentlyExcluded"))
          val result = controller invokePrivate privateMethodCall(etmpExclusionWithReversal)

          result mustBe false
        }
      }

      "must return true if there is an exclusion that is not a reversal" in {

        val etmpExclusionWithoutReversal: Seq[EtmpExclusion] = Seq(arbitraryEtmpExclusion.arbitrary.sample.value
          .copy(exclusionReason = EtmpExclusionReason.NoLongerSupplies))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {

          val controller = application.injector.instanceOf[SubmittedReturnForPeriodController]

          val privateMethodCall = PrivateMethod[Boolean](Symbol("isCurrentlyExcluded"))
          val result = controller invokePrivate privateMethodCall(etmpExclusionWithoutReversal)

          result mustBe true
        }
      }
    }
  }
}
