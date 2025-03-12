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

package controllers

import base.SpecBase
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import connectors.{SaveForLaterConnector, SavedUserAnswers}
import models.audit.{ReturnsAuditModel, SubmissionResult}
import models.etmp.EtmpExclusionReason.TransferringMSID
import models.etmp.{EtmpDisplayRegistration, EtmpExclusion}
import models.requests.DataRequest
import models.{Country, RegistrationWrapper, TotalVatToCountry, UserAnswers, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.*
import pages.{CheckYourAnswersPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatRatesFromCountryPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}
import services.{AuditService, CoreVatReturnService, PartialReturnPeriodService, SalesAtVatRateService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryList, SummaryListRow}
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.corrections.{CorrectPreviousReturnSummary, CorrectionNoPaymentDueSummary, CorrectionReturnPeriodSummary}
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value

  private val mockSalesAtVatRateService = mock[SalesAtVatRateService]
  private val mockCoreVatReturnService = mock[CoreVatReturnService]
  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]
  private val mockAuditService = mock[AuditService]
  private val mockSaveForLaterConnector = mock[SaveForLaterConnector]
  private val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockSalesAtVatRateService)
    Mockito.reset(mockCoreVatReturnService)
    Mockito.reset(mockPartialReturnPeriodService)
    Mockito.reset(mockAuditService)
    Mockito.reset(mockSaveForLaterConnector)
    super.beforeEach()
  }

  "Check Your Answers Controller" - {

    "onPageLoad" - {

      "when correct previous return is false / empty" - {

        "must return OK and the correct view for a GET" in {

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage(0), false).success.value))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) `mustBe` OK
            contentAsString(result).contains("Business name") `mustBe` true
            contentAsString(result).contains("UK VAT registration number") `mustBe` true
            contentAsString(result).contains("Return month") `mustBe` true
            contentAsString(result).contains("Sales to EU countries, Northern Ireland or both") `mustBe` true
            contentAsString(result).contains("Sales made") `mustBe` true
            contentAsString(result).contains("Sales excluding VAT") `mustBe` true
            contentAsString(result).contains("Corrections") `mustBe` true
            contentAsString(result).contains("VAT owed") `mustBe` true
            contentAsString(result).contains("Total VAT payable") `mustBe` true
          }
        }

        "must return OK and the correct view for a GET when the correction choice was NO " in {

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage(0), false).success.value))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) `mustBe` OK
            contentAsString(result).contains("Business name") `mustBe` true
            contentAsString(result).contains("UK VAT registration number") `mustBe` true
            contentAsString(result).contains("Return month") `mustBe` true
            contentAsString(result).contains("Sales to EU countries, Northern Ireland or both") `mustBe` true
            contentAsString(result).contains("Sales made") `mustBe` true
            contentAsString(result).contains("Sales excluding VAT") `mustBe` true
            contentAsString(result).contains("Corrections") `mustBe` true
            contentAsString(result).contains("VAT owed") `mustBe` true
            contentAsString(result).contains("Total VAT payable") `mustBe` true
          }
        }

        "must return OK and the correct view for a GET when there is an exclusion present and it's the trader's final return" in {

          val etmpExclusion: EtmpExclusion = EtmpExclusion(
            exclusionReason = TransferringMSID,
            effectiveDate = period.firstDay,
            decisionDate = period.firstDay,
            quarantine = false
          )

          val registration: EtmpDisplayRegistration = registrationWrapper.registration.copy(exclusions = Seq(etmpExclusion))

          val updatedRegistrationWrapper: RegistrationWrapper = registrationWrapper.copy(registration = registration)

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(eqTo(updatedRegistrationWrapper), eqTo(period))(any())) thenReturn None.toFuture
          when(mockPartialReturnPeriodService.isFinalReturn(any(), any())) thenReturn true

          when(mockSalesAtVatRateService.getTotalNetSales(any())) thenReturn None
          when(mockSalesAtVatRateService.getTotalVatOnSales(any())) thenReturn None
          when(mockSalesAtVatRateService.getVatOwedToCountries(any())) thenReturn List.empty
          when(mockSalesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn BigDecimal(0)

          val answers: UserAnswers = completeUserAnswers
            .set(SoldGoodsPage, false).success.value
            .set(CorrectPreviousReturnPage(0), false).success.value

          val application = applicationBuilder(userAnswers = Some(answers), registration = updatedRegistrationWrapper)
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .overrides(bind[SalesAtVatRateService].toInstance(mockSalesAtVatRateService))
            .build()

          running(application) {
            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val businessSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                BusinessNameSummary.row(registrationWrapper),
                BusinessVRNSummary.row(vrn),
                ReturnPeriodSummary.row(answers, waypoints, Some(period))
              ).flatten
            ).withCssClass("govuk-summary-card govuk-summary-card__content govuk-!-display-block width-auto")

            val salesFromEuSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                SoldGoodsSummary.row(answers, waypoints, CheckYourAnswersPage),
                TotalNetValueOfSalesSummary.row(answers, None, waypoints, CheckYourAnswersPage),
                TotalVatOnSalesSummary.row(answers, None, waypoints, CheckYourAnswersPage)
              ).flatten
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("checkYourAnswers.sales.heading"))))
              )
            )

            val correctionsSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                CorrectPreviousReturnSummary.row(answers, waypoints, CheckYourAnswersPage),
                CorrectionReturnPeriodSummary.getAllRows(answers, waypoints, CheckYourAnswersPage)
              ).flatten
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("checkYourAnswers.correction.heading"))))
              )
            )

            val allSummaryLists = Seq(
              (None, businessSummaryList),
              (None, salesFromEuSummaryList),
              (None, correctionsSummaryList)
            )

            val noPaymentDueSummaryList: Seq[SummaryListRow] = CorrectionNoPaymentDueSummary.row(List.empty)
            val totalVatOnSales: BigDecimal = BigDecimal(0)

            status(result) `mustBe` OK
            contentAsString(result) `mustBe` view(
              waypoints = waypoints,
              summaryLists = allSummaryLists,
              period = period,
              totalVatToCountries = List.empty,
              totalVatOnSales = totalVatOnSales,
              noPaymentDueSummaryList = noPaymentDueSummaryList,
              containsCorrections = false,
              missingData = List.empty,
              maybeExclusion = Some(etmpExclusion),
              isFinalReturn = true
            )(request, messages(application)).toString
          }
        }
      }

      "when correct previous return is true" - {

        "must contain VAT declared to EU countries after corrections heading if there were corrections and all totals are positive" in {

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(completeUserAnswers.set(CorrectPreviousReturnPage(0), true).success.value))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val result = route(application, request).value

            status(result) `mustBe` OK
            contentAsString(result).contains("Business name") `mustBe` true
            contentAsString(result).contains("UK VAT registration number") `mustBe` true
            contentAsString(result).contains("Return month") `mustBe` true
            contentAsString(result).contains("Sales to EU countries, Northern Ireland or both") `mustBe` true
            contentAsString(result).contains("Sales made") `mustBe` true
            contentAsString(result).contains("Sales excluding VAT") `mustBe` true
            contentAsString(result).contains("Corrections") `mustBe` true
            contentAsString(result).contains("VAT owed") `mustBe` true
            contentAsString(result).contains("Total VAT payable") `mustBe` true
          }
        }

        "must return OK and the correct view for a GET when there are negative totals within corrections" in {

          val previouslyDeclaredCorrectionAmount: BigDecimal = BigDecimal(1500)
          val previouslyDeclaredNegativeCorrectionAmount: BigDecimal = BigDecimal(-1000)

          val userAnswersWithCorrections: UserAnswers = emptyUserAnswers
            .set(SoldGoodsPage, false).success.value
            .set(CorrectPreviousReturnPage(0), true).success.value
            .set(CorrectionReturnYearPage(index), period.year).success.value
            .set(CorrectionReturnPeriodPage(index), period).success.value
            .set(CorrectionCountryPage(index, index), country).success.value
            .set(
              PreviouslyDeclaredCorrectionAmountQuery(index, index),
              PreviouslyDeclaredCorrectionAmount(previouslyDeclared = true, amount = previouslyDeclaredCorrectionAmount)
            ).success.value
            .set(VatAmountCorrectionCountryPage(index, index), previouslyDeclaredNegativeCorrectionAmount).success.value
            .set(VatPayableForCountryPage(index, index), true).success.value

          val totalVatToCountries: List[TotalVatToCountry] = List.empty
          val noPaymentsDue: List[TotalVatToCountry] = List(TotalVatToCountry(country = country, totalVat = previouslyDeclaredNegativeCorrectionAmount))
          val totalVatOnSales: BigDecimal = BigDecimal(0)

          when(mockSalesAtVatRateService.getTotalNetSales(any())) thenReturn None
          when(mockSalesAtVatRateService.getTotalVatOnSales(any())) thenReturn None
          when(mockSalesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn BigDecimal(0)
          when(mockSalesAtVatRateService.getVatOwedToCountries(eqTo(userAnswersWithCorrections))) thenReturn (noPaymentsDue ++ totalVatToCountries)
          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(userAnswersWithCorrections))
            .overrides(bind[SalesAtVatRateService].toInstance(mockSalesAtVatRateService))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {

            implicit val msgs: Messages = messages(application)

            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

            val view = application.injector.instanceOf[CheckYourAnswersView]

            val result = route(application, request).value

            val businessSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                BusinessNameSummary.row(registrationWrapper),
                BusinessVRNSummary.row(vrn),
                ReturnPeriodSummary.row(userAnswersWithCorrections, waypoints, Some(period))
              ).flatten
            ).withCssClass("govuk-summary-card govuk-summary-card__content govuk-!-display-block width-auto")

            val salesFromEuSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                SoldGoodsSummary.row(userAnswersWithCorrections, waypoints, CheckYourAnswersPage),
                TotalNetValueOfSalesSummary.row(userAnswersWithCorrections, None, waypoints, CheckYourAnswersPage),
                TotalVatOnSalesSummary.row(userAnswersWithCorrections, None, waypoints, CheckYourAnswersPage)
              ).flatten
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("checkYourAnswers.sales.heading"))))
              )
            )

            val correctionsSummaryList: SummaryList = SummaryListViewModel(
              rows = Seq(
                CorrectPreviousReturnSummary.row(userAnswersWithCorrections, waypoints, CheckYourAnswersPage),
                CorrectionReturnPeriodSummary.getAllRows(userAnswersWithCorrections, waypoints, CheckYourAnswersPage)
              ).flatten
            ).withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(msgs("checkYourAnswers.correction.heading"))))
              )
            )

            val allSummaryLists = Seq(
              (None, businessSummaryList),
              (None, salesFromEuSummaryList),
              (None, correctionsSummaryList)
            )

            val noPaymentsDueSummaryList: Seq[SummaryListRow] = CorrectionNoPaymentDueSummary.row(noPaymentsDue)

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(
                waypoints,
                allSummaryLists,
                period,
                totalVatToCountries,
                totalVatOnSales,
                noPaymentsDueSummaryList,
                containsCorrections = true,
                List.empty,
                None,
                isFinalReturn = false
              )(request, messages(application)).toString
          }
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "when the user answered all necessary data and submission of the return succeeds" in {

        val remainingAmount = BigDecimal("2.22")

        when(mockCoreVatReturnService.submitCoreVatReturn(any())(any())) thenReturn
          Future.successful(remainingAmount)

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[CoreVatReturnService].toInstance(mockCoreVatReturnService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = false).url)

          val result = route(application, request).value

          implicit val dataRequest: DataRequest[_] =
            DataRequest(request, testCredentials, vrn, userAnswersId, registrationWrapper, userAnswers)

          val expectedAuditEvent = ReturnsAuditModel.build(userAnswers, SubmissionResult.Success)

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.submissionResults.routes.SuccessfullySubmittedController.onPageLoad().url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }

      "when the user answered all necessary data and submission of the return fails" in {
        when(mockCoreVatReturnService.submitCoreVatReturn(any())(any())) thenReturn
          Future.failed(new RuntimeException("Failed submission"))

        when(mockSaveForLaterConnector.submit(any())(any())) thenReturn
          Future.successful(Right(Some(mock[SavedUserAnswers])))

        val userAnswers = completeUserAnswers
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[CoreVatReturnService].toInstance(mockCoreVatReturnService))
          .overrides(bind[AuditService].toInstance(mockAuditService))
          .overrides(bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = false).url)

          val result = route(application, request).value

          implicit val dataRequest: DataRequest[_] =
            DataRequest(request, testCredentials, vrn, userAnswersId, registrationWrapper, userAnswers)

          val expectedAuditEvent = ReturnsAuditModel.build(userAnswers, SubmissionResult.Failure)

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.submissionResults.routes.ReturnSubmissionFailureController.onPageLoad().url
          verify(mockAuditService, times(1)).audit(eqTo(expectedAuditEvent))(any(), any())
        }
      }
    }

    "when the user has not answered" - {

      "a question but the missing data prompt has not been shown, must refresh page" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = false).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.CheckYourAnswersController.onPageLoad(waypoints).url
        }
      }

      "country of consumption must redirect to SoldToCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.SoldToCountryController.onPageLoad(waypoints, index).url
        }
      }

      "vat rates, must redirect to VatRatesFromCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), Country.euCountries.head).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.VatRatesFromCountryController.onPageLoad(waypoints, index).url
        }
      }

      "net value of sales must redirect to SalesToCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), Country.euCountries.head).success.value
          .set(VatRatesFromCountryPage(index, index), List[VatRateFromCountry](vatRateFromCountry)).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.SalesToCountryController.onPageLoad(waypoints, index, index).url
        }
      }

      "vat on sales must redirect to VatOnSalesController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, true).success.value
          .set(SoldToCountryPage(index), Country.euCountries.head).success.value
          .set(VatRatesFromCountryPage(index, index), List[VatRateFromCountry](vatRateFromCountry)).success.value
          .set(SalesToCountryPage(index, index), salesValue).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` routes.VatOnSalesController.onPageLoad(waypoints, index, index).url
        }
      }

      "year of correct must redirect to CorrectionReturnYearController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value
          .set(CorrectPreviousReturnPage(0), true).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.corrections.routes.CorrectionReturnYearController.onPageLoad(waypoints, index).url
        }
      }

      "country of correction must redirect to CorrectionCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value
          .set(CorrectPreviousReturnPage(0), true).success.value
          .set(CorrectionReturnYearPage(index), period.year).success.value
          .set(CorrectionReturnPeriodPage(index), period).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.corrections.routes.CorrectionCountryController.onPageLoad(waypoints, index, index).url
        }
      }

      "amount of correction must redirect to VatAmountCorrectionCountryController" in {

        val answers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value
          .set(CorrectPreviousReturnPage(0), true).success.value
          .set(CorrectionReturnPeriodPage(index), period).success.value
          .set(CorrectionCountryPage(index, index), Country.euCountries.head).success.value

        val app = applicationBuilder(Some(answers)).build()

        running(app) {
          val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit(waypoints, incompletePromptShown = true).url)
          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.corrections.routes.VatAmountCorrectionCountryController.onPageLoad(waypoints, index, index).url
        }
      }
    }
  }
}
