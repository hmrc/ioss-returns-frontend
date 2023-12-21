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

package services

import base.SpecBase
import connectors.{FinancialDataConnector, PaymentConnector, VatReturnConnector}
import models.Period
import models.etmp._
import models.financialdata.{FinancialData, FinancialTransaction, Item}
import models.payments.Payment
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import uk.gov.hmrc.http.HeaderCarrier

import java.time._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with PaymentsServiceSpecFixture with ScalaCheckPropertyChecks {
  implicit val hc = HeaderCarrier()
  val paymentConnector = mock[PaymentConnector]
  "PaymentsService" - {

    "must return payments when there are due payments and overdue payments - from FinancialData, vatReturn to be ignored" in {

      val periodOverdue = Period(2021, Month.JANUARY)
      val periodDue = Period(2021, Month.APRIL)

      val transactionAmountDue1 = 250
      val transactionAmountDue2 = 750
      val transactionAmountOverdue1 = 150
      val transactionAmountOverdue2 = 350

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountDue1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountDue2)))
      val ft3 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = None)
      val ft4 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountOverdue1)))
      val ft5 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmountOverdue2)))
      val ft6 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue.firstDay), outstandingAmount = None)

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4, ft5, ft6)))

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]
      val periodOverdueKey = "21AA"
      val periodDueKey = "21AD"

      val vatReturnOverdue = vatReturn.copy(periodKey = periodOverdueKey)
      val vatReturnDue = vatReturn.copy(periodKey = periodDueKey)

      when(vatReturnConnector.get(Period.fromKey(periodOverdueKey))(hc))
        .thenReturn(Future.successful(Right(vatReturnOverdue)))
      when(vatReturnConnector.get(Period.fromKey(periodDueKey))(hc))
        .thenReturn(Future.successful(Right(vatReturnDue)))
      when(financialDataConnector.getFinancialData(any())(any()))
        .thenReturn(Future.successful(inputFinancialData))

      val obligationsDetails = obligationsResponse.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodOverdueKey)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodDueKey)

      when(vatReturnConnector.getObligations(any())(any()))
        .thenReturn(Future.successful(obligationsResponse.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector, paymentConnector)

      val result = service.getUnpaidPayments("iossNumber")

      whenReady(result) { r =>
        val paymentOverdue = service.calculatePayment(vatReturnOverdue, Some(inputFinancialData))
        val paymentDue = service.calculatePayment(vatReturnDue, Some(inputFinancialData))

        r mustBe List(paymentOverdue, paymentDue)
        paymentOverdue.amountOwed mustBe (transactionAmountOverdue1 + transactionAmountOverdue2)
        paymentDue.amountOwed mustBe (transactionAmountDue1 + transactionAmountDue2)
      }
    }

    "must return correct payment when there are due payments - from FinancialData, vatReturn to be ignored" in {

      val periodDue = Period(2021, Month.SEPTEMBER)

      val transactionAmount1 = 250
      val transactionAmount2 = 750

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodDue.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount2)))

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2)))
      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val periodKey1 = "21AI"

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)

      when(vatReturnConnector.get(Period.fromKey(periodKey1))(hc))
        .thenReturn(Future.successful(Right(vatReturn1)))

      when(financialDataConnector.getFinancialData(any())(any())).thenReturn(Future.successful(inputFinancialData))

      val obligationsDetails = obligationsResponse.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)

      when(vatReturnConnector.getObligations(any())(any()))
        .thenReturn(Future.successful(obligationsResponse.copy(obligationDetails = List(obligationsDetail1))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector, paymentConnector)

      val payment = service.calculatePayment(vatReturn1, Some(inputFinancialData))

      val result: Future[List[Payment]] = service.getUnpaidPayments("iossNumber")

      whenReady(result) { r =>
        r mustBe List(payment)
        payment.amountOwed mustBe (transactionAmount1 + transactionAmount2)
      }
    }

    "must return payments when there are overdue payments - from FinancialData, vatReturn to be ignored" in {

      val periodOverdue1 = Period(2021, Month.JUNE)
      val periodOverdue2 = Period(2021, Month.SEPTEMBER)

      val periodKey1 = "21AI"
      val periodKey2 = "21AF"

      val transactionAmount1 = 250
      val transactionAmount2 = 750
      val transactionAmount3 = 300
      val transactionAmount4 = 700

      val ft1 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue1.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val ft2 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue1.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount2)))
      val ft3 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue2.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount3)))
      val ft4 = financialTransaction
        .copy(taxPeriodFrom = Some(periodOverdue2.firstDay), outstandingAmount = Some(BigDecimal(transactionAmount4)))

      val inputFinancialData = financialData.copy(financialTransactions = Some(List(ft1, ft2, ft3, ft4)))

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)
      val vatReturn2 = vatReturn.copy(periodKey = periodKey2)

      when(vatReturnConnector.get(Period.fromKey(periodKey1))(hc))
        .thenReturn(Future.successful(Right(vatReturn1)))
      when(vatReturnConnector.get(Period.fromKey(periodKey2))(hc))
        .thenReturn(Future.successful(Right(vatReturn2)))
      when(financialDataConnector.getFinancialData(any())(any())).thenReturn(Future.successful(inputFinancialData))

      val obligationsDetails = obligationsResponse.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodKey2)

      when(vatReturnConnector.getObligations(any())(any()))
        .thenReturn(Future.successful(obligationsResponse.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector, paymentConnector)

      val result = service.getUnpaidPayments("iossNumber")

      val payment1 = service.calculatePayment(vatReturn1, Some(inputFinancialData))
      val payment2 = service.calculatePayment(vatReturn2, Some(inputFinancialData))

      whenReady(result) { r =>
        r mustBe (List(payment1, payment2))
        payment1.amountOwed mustBe (transactionAmount1 + transactionAmount2)
        payment2.amountOwed mustBe (transactionAmount3 + transactionAmount4)
      }
    }

    "must return payments when there are overdue payments - from FinancialData with no transactions, vatReturn to be taken into account" - {

      val periodKey1 = "21AI"
      val periodKey2 = "21AF"

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val vatReturn1 = vatReturn.copy(periodKey = periodKey1)
      val vatReturn2 = vatReturn.copy(periodKey = periodKey2)

      when(vatReturnConnector.get(Period.fromKey(periodKey1))(hc))
        .thenReturn(Future.successful(Right(vatReturn1)))
      when(vatReturnConnector.get(Period.fromKey(periodKey2))(hc))
        .thenReturn(Future.successful(Right(vatReturn2)))

      val obligationsDetails = obligationsResponse.obligationDetails
      val obligationsDetail1: EtmpObligationDetails = obligationsDetails(0).copy(periodKey = periodKey1)
      val obligationsDetail2: EtmpObligationDetails = obligationsDetails(1).copy(periodKey = periodKey2)

      when(vatReturnConnector.getObligations(any())(any()))
        .thenReturn(Future.successful(obligationsResponse.copy(obligationDetails = List(obligationsDetail1, obligationsDetail2))))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector, paymentConnector)

      val inputFinancialDataWithTransactionsNone = financialData.copy(financialTransactions = None)
      val inputFinancialDataWithTransactionsSomeNil = financialData.copy(financialTransactions = Some(Nil))
      val scenarios = Table[FinancialData, String](
        ("FinancialData", "Title"),
        (inputFinancialDataWithTransactionsNone, "inputFinancialDataWithTransactionsNone"),
        (inputFinancialDataWithTransactionsSomeNil, "inputFinancialDataWithTransactionsSomeNil")
      )

      forAll(scenarios) { (inputFinancialData, title) => {
        when(financialDataConnector.getFinancialData(any())(any())).thenReturn(Future.successful(inputFinancialData))

        val result = service.getUnpaidPayments("iossNumber")

        val payment1 = service.calculatePayment(vatReturn1, Some(inputFinancialData))
        val payment2 = service.calculatePayment(vatReturn2, Some(inputFinancialData))

        s"when $title" in {
          whenReady(result) { r =>
            r mustBe (List(payment1, payment2))
            payment1.amountOwed mustBe vatReturn1.getTotalVatOnSalesAfterCorrection()
            payment2.amountOwed mustBe vatReturn2.getTotalVatOnSalesAfterCorrection()
          }
        }
      }
      }
    }

    "filterIfPaymentOutstanding correctly: " +
      "if some payments 'FOR THAT PERIOD' made with outstanding amount " +
      "or no payments made 'FOR THAT PERIOD' but there is vat amount 'FOR THAT PERIOD'" in {

      val inputFinancialDataWithNoTransactions = financialData.copy(financialTransactions = None)

      val vatReturnConnector = mock[VatReturnConnector]
      val financialDataConnector = mock[FinancialDataConnector]

      val periodKey1 = "21AI"

      val vatReturnWithNoGoodsSuppliedAndNoCorrection = vatReturn.copy(
        periodKey = periodKey1, goodsSupplied = Nil, totalVATAmountFromCorrectionGBP = BigDecimal(0))

      val service = new PaymentsService(financialDataConnector, vatReturnConnector, paymentConnector)

      val vatCorrectionAmount = 50

      val vatReturnWithNoGoodsSuppliedAndSomeCorrection = vatReturnWithNoGoodsSuppliedAndNoCorrection
        .copy(totalVATAmountFromCorrectionGBP = BigDecimal(vatCorrectionAmount))

      val transactionAmount1 = 250

      val transactionNotInPeriod = financialTransaction
        .copy(taxPeriodFrom = Some(Period.fromKey(periodKey1).firstDay.minusMonths(1)), outstandingAmount = Some(BigDecimal(transactionAmount1)))
      val transactionInPeriod = financialTransaction
        .copy(taxPeriodFrom = Some(Period.fromKey(periodKey1).firstDay), outstandingAmount = Some(BigDecimal(transactionAmount1)))

      val inputFinancialDataWithSomeTransactionsButNotInPeriod = inputFinancialDataWithNoTransactions
        .copy(financialTransactions = Some(List(transactionNotInPeriod)))
      val inputFinancialDataWithSomeTransactionsInPeriod = inputFinancialDataWithNoTransactions.copy(financialTransactions = Some(List(transactionInPeriod)))

      val scenarios = Table[Option[FinancialData], List[EtmpVatReturn], List[EtmpVatReturn]](
        ("FinancialData", "EtmpVatReturn", "Filtered EtmpVatReturn"),
        (
          Some(inputFinancialDataWithNoTransactions),
          List(vatReturnWithNoGoodsSuppliedAndNoCorrection),
          Nil
        ),
        (
          Some(inputFinancialDataWithNoTransactions),
          List(vatReturnWithNoGoodsSuppliedAndSomeCorrection),
          List(vatReturnWithNoGoodsSuppliedAndSomeCorrection)
        ),
        (
          Some(inputFinancialDataWithSomeTransactionsButNotInPeriod),
          List(vatReturnWithNoGoodsSuppliedAndNoCorrection),
          Nil
        ),
        (
          Some(inputFinancialDataWithSomeTransactionsInPeriod),
          List(vatReturnWithNoGoodsSuppliedAndNoCorrection),
          List(vatReturnWithNoGoodsSuppliedAndNoCorrection)
        )
      )

      forAll(scenarios) { (fd, vr, r) =>
        val result = service.filterIfPaymentOutstanding(fd, vr)
        result mustBe r
      }
    }
  }
}

trait PaymentsServiceSpecFixture {
  protected val zonedNow: ZonedDateTime = ZonedDateTime.of(
    2023,
    2,
    1,
    0,
    0,
    0,
    0,
    ZoneOffset.UTC
  )

  protected val zonedDateTimeNow = ZonedDateTime.now().plusSeconds(1)

  protected val dateFrom: LocalDate = zonedNow.toLocalDate.minusMonths(1)
  protected val dateTo: LocalDate = zonedNow.toLocalDate

  protected val item = Item(Some(500), Some(""), Some(""), Some(500), Some(""))
  protected val financialTransaction = FinancialTransaction(
    Some("G Ret AT EU-OMS"), None, Some(dateFrom), Some(dateTo), Some(1000), Some(500), Some(500), Some(Seq(item)))

  protected val vatReturn = EtmpVatReturn(
    returnReference = "XI/IM9001234567/2023.M11",
    periodKey = "23AK",
    returnPeriodFrom = LocalDate.of(2023, 1, 1),
    returnPeriodTo = LocalDate.of(2023, 1, 31),
    goodsSupplied = Seq(
      EtmpVatReturnGoodsSupplied(
        msOfConsumption = "FR",
        vatRateType = EtmpVatRateType.ReducedVatRate,
        taxableAmountGBP = BigDecimal(12345.67),
        vatAmountGBP = BigDecimal(2469.13)
      )
    ),
    totalVATGoodsSuppliedGBP = BigDecimal(2469.13),
    totalVATAmountPayable = BigDecimal(2469.13),
    totalVATAmountPayableAllSpplied = BigDecimal(2469.13),
    correctionPreviousVATReturn = Seq(
      EtmpVatReturnCorrection(
        periodKey = "23AJ",
        periodFrom = LocalDate.of(2023, 1, 1).toString,
        periodTo = LocalDate.of(2023, 1, 31).toString,
        msOfConsumption = "FR"
      )
    ),
    totalVATAmountFromCorrectionGBP = BigDecimal(100.00),
    balanceOfVATDueForMS = Seq(
      EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = "FR",
        totalVATDueGBP = BigDecimal(2569.13),
        totalVATEUR = BigDecimal(2569.13)
      )
    ),
    totalVATAmountDueForAllMSEUR = BigDecimal(2569.13),
    paymentReference = "XI/IM9001234567/2023.M11"

  )

  protected val obligationsResponse = EtmpObligations(
    referenceNumber = "idNumber",
    referenceType = "regimeType",
    obligationDetails = Seq(
      EtmpObligationDetails(
        status = EtmpObligationsFulfilmentStatus.Open,
        periodKey = "23AL"
      ),
      EtmpObligationDetails(
        status = EtmpObligationsFulfilmentStatus.Open,
        periodKey = "23AK"
      )
    )
  )

  protected val financialData = FinancialData(Some("IOSS"), Some("123456789"), Some("ECOM"), zonedDateTimeNow, Some(Seq(financialTransaction)))
}