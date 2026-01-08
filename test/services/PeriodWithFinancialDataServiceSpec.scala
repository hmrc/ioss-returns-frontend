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

package services

import base.SpecBase
import connectors.VatReturnConnector
import models.Period
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import models.payments.PrepareData
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.{Clock, LocalDate, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class PeriodWithFinancialDataServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val hc: HeaderCarrier = HeaderCarrier()
  private val mockPaymentsService: PaymentsService = mock[PaymentsService]
  private val mockObligationsService: ObligationsService = mock[ObligationsService]
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  override def beforeEach(): Unit = {
    reset(mockPaymentsService)
    reset(mockObligationsService)
    reset(mockVatReturnConnector)
  }

  "PeriodWithFinancialDataService" - {

    ".getPeriodWithFinancialData" - {

      "must return data with periods no greater than 6 years from the last day of the current period" in {

        val today = LocalDate.of(2024, 9, 4).atStartOfDay(ZoneId.systemDefault()).toInstant
        val clock = Clock.fixed(today, ZoneId.systemDefault())

        val obligationDetails: Seq[EtmpObligationDetails] = Seq(
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Fulfilled,
            periodKey = "18AJ"
          ),
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Fulfilled,
            periodKey = "18AI"
          ),
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Fulfilled,
            periodKey = "18AH"
          ),
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Fulfilled,
            periodKey = "18AG"
          )
        )

        val periods = obligationDetails.map(obligationDetails => Period.fromKey(obligationDetails.periodKey))

        val expectedPeriodWithFinancialData = periods.map {
          period => (period, arbitraryPayment.arbitrary.sample.value.copy(period = period))
        }.groupBy(_._1.year)

        val payments = expectedPeriodWithFinancialData.flatMap {
          case (_ -> x) => x.map(_._2)
        }

        val prepareData: PrepareData = PrepareData(
          duePayments = payments.toList,
          overduePayments = List.empty,
          excludedPayments = List.empty,
          totalAmountOwed = 0,
          totalAmountOverdue = 0,
          iossNumber = iossNumber
        )

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialDataWithIossNumber(any())(any(), any())) thenReturn prepareData.toFuture

        val service = new PeriodWithFinancialDataService(clock, mockPaymentsService, mockObligationsService, mockVatReturnConnector)

        val expectedResult = expectedPeriodWithFinancialData.map {
          case (year, payments) => (year, payments.take(2))
        }

        val result = service.getPeriodWithFinancialData(iossNumber)(hc).futureValue

        result mustBe expectedResult
      }
    }
  }
}
