/*
 * Copyright 2025 HM Revenue & Customs
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
import models.etmp.EtmpVatReturn
import models.payments.{Payment, PaymentStatus, PrepareData}
import models.{Period, StandardPeriod, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ObligationsService, PaymentsService, PreviousRegistrationService}
import testUtils.EtmpVatReturnData.etmpVatReturn
import testUtils.PeriodWithFinancialData._
import testUtils.PreviousRegistrationData.previousRegistrations
import utils.FutureSyntax.FutureOps
import views.html.previousReturns.SubmittedReturnsHistoryView

import java.time.Month
import scala.concurrent.Future

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockPaymentsService: PaymentsService = mock[PaymentsService]
  private val mockPreviousRegistrationService: PreviousRegistrationService = mock[PreviousRegistrationService]
  private val mockObligationsService: ObligationsService = mock[ObligationsService]
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentsService)
    Mockito.reset(mockPreviousRegistrationService)
    Mockito.reset(mockObligationsService)
    Mockito.reset(mockVatReturnConnector)
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and the correct view for a GET when there are submitted returns" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, periodsWithFinancialData, previousRegistrations, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are no submitted returns" in {

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = Map.empty

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq.empty.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn emptyPrepareData.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, periodsWithFinancialData, previousRegistrations, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when using data from vat return for a paid return" in {

      val payments: List[Payment] =
        obligationPeriods.map { period =>
          Payment(
            period = period,
            amountOwed = BigDecimal(0),
            dateDue = period.paymentDeadline,
            paymentStatus = PaymentStatus.Paid
          )
        }.toList

      val prepareData: PrepareData = {
        PrepareData(
          duePayments = List(payments.head.copy(period = StandardPeriod(2023, Month.APRIL))),
          overduePayments = payments.tail,
          excludedPayments = List.empty,
          totalAmountOwed = payments.map(_.amountOwed).sum,
          totalAmountOverdue = BigDecimal(0),
          iossNumber = iossNumber
        )
      }

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = obligationPeriods.flatMap { period =>
        Map(period -> payments.filter(_.period == period).head)
      }.groupBy(_._1.year)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(etmpVatReturn).toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, periodsWithFinancialData, previousRegistrations, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when using data from vat return for a nil return" in {

      val emptyCorrectionsAndGoodsSuppliedVatReturn: EtmpVatReturn =
        etmpVatReturn.copy(
          goodsSupplied = Seq.empty,
          correctionPreviousVATReturn = Seq.empty
        )

      val payments: List[Payment] =
        obligationPeriods.map { period =>
          Payment(
            period = period,
            amountOwed = BigDecimal(0),
            dateDue = period.paymentDeadline,
            paymentStatus = PaymentStatus.NilReturn
          )
        }.toList

      val prepareData: PrepareData = {
        PrepareData(
          duePayments = List(payments.head.copy(period = StandardPeriod(2023, Month.JUNE))),
          overduePayments = payments.tail,
          excludedPayments = List.empty,
          totalAmountOwed = payments.map(_.amountOwed).sum,
          totalAmountOverdue = BigDecimal(0),
          iossNumber = iossNumber
        )
      }

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = obligationPeriods.flatMap { period =>
        Map(period -> payments.filter(_.period == period).head)
      }.groupBy(_._1.year)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Right(emptyCorrectionsAndGoodsSuppliedVatReturn).toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, periodsWithFinancialData, previousRegistrations, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are exclusions" in {

      val payments: List[Payment] =
        obligationPeriods.map { period =>
          Payment(
            period = period,
            amountOwed = BigDecimal(0),
            dateDue = period.paymentDeadline,
            paymentStatus = PaymentStatus.Excluded
          )
        }.toList

      val prepareData: PrepareData = {
        PrepareData(
          duePayments = List(payments.head.copy(period = StandardPeriod(2023, Month.JUNE))),
          overduePayments = payments.tail,
          excludedPayments = List(payments.head),
          totalAmountOwed = payments.map(_.amountOwed).sum,
          totalAmountOverdue = BigDecimal(0),
          iossNumber = iossNumber
        )
      }

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = obligationPeriods.flatMap { period =>
        Map(period -> payments.filter(_.period == period).head)
      }.groupBy(_._1.year)


      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(waypoints, periodsWithFinancialData, previousRegistrations, isIntermediary = false, companyName = "Company Name")(request, messages(application)).toString
      }
    }

    "must throw an exception when financial data API is down" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn Future.failed(new Exception("Some exception"))
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exp => exp mustBe a[Exception] }
      }
    }

    "must throw an Illegal State Exception when server responds with an error when retrieving an ETMP vat return" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .overrides(bind[PreviousRegistrationService].toInstance(mockPreviousRegistrationService))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn emptyPrepareData.toFuture
        when(mockPreviousRegistrationService.getPreviousRegistrations(any())(any())) thenReturn previousRegistrations.toFuture
        when(mockVatReturnConnector.getForIossNumber(any(), any())(any())) thenReturn Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value.failed

        whenReady(result) { exp =>
          exp mustBe a[IllegalStateException]
          exp.getMessage must include(exp.getLocalizedMessage)
        }
      }
    }
  }
}
