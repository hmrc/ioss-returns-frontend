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

package services

import base.SpecBase
import connectors.{FinancialDataConnector, RegistrationConnector}
import models.enrolments.EACDEnrolments
import models.payments.PrepareData
import models.{StandardPeriod, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps
import viewmodels.previousReturns.PreviousRegistration

import java.time.YearMonth
import scala.concurrent.ExecutionContext.Implicits.global

class PreviousRegistrationServiceSpec extends SpecBase with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val eACDEnrolments: EACDEnrolments = arbitraryEACDEnrolments.arbitrary.sample.value

  private val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  private val mockFinancialDataConnector: FinancialDataConnector = mock[FinancialDataConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockRegistrationConnector)
    Mockito.reset(mockFinancialDataConnector)
  }

  "PreviousRegistrationService" - {

    ".getPreviousRegistrations" - {

      val previousRegistration: PreviousRegistration =
        PreviousRegistration(
          iossNumber = eACDEnrolments.enrolments.head.identifiers.head.value,
          startPeriod = StandardPeriod(YearMonth.from(eACDEnrolments.enrolments.head.activationDate.get)),
          endPeriod = StandardPeriod(YearMonth.from(eACDEnrolments.enrolments.head.activationDate.get.minusMonths(1)))
        )

      "must return List(PreviousRegistration) when valid data successfully retrieved" in {

        when(mockRegistrationConnector.getAccounts()) thenReturn eACDEnrolments.toFuture

        val service = new PreviousRegistrationService(mockRegistrationConnector, mockFinancialDataConnector)

        val result = service.getPreviousRegistrations().futureValue

        result mustBe List(previousRegistration)
      }
    }

    ".getPreviousRegistrationPrepareFinancialData" - {

      val prepareData: PrepareData = arbitraryPrepareData.arbitrary.sample.value
        .copy(iossNumber = eACDEnrolments.enrolments.tail.head.identifiers.head.value)

      "must return List(PrepareData) when valid data successfully retrieved" in {

        when(mockRegistrationConnector.getAccounts()) thenReturn eACDEnrolments.toFuture
        when(mockFinancialDataConnector.prepareFinancialDataWithIossNumber(any())(any())) thenReturn Right(prepareData).toFuture

        val service = new PreviousRegistrationService(mockRegistrationConnector, mockFinancialDataConnector)

        val result = service.getPreviousRegistrationPrepareFinancialData().futureValue

        result mustBe List(prepareData)
      }

      "must throw an Exception when connector returns Left(error)" in {

        when(mockRegistrationConnector.getAccounts()) thenReturn eACDEnrolments.toFuture
        when(mockFinancialDataConnector.prepareFinancialDataWithIossNumber(any())(any())) thenReturn
          Left(UnexpectedResponseStatus(INTERNAL_SERVER_ERROR, "error")).toFuture

        val service = new PreviousRegistrationService(mockRegistrationConnector, mockFinancialDataConnector)

        val result = service.getPreviousRegistrationPrepareFinancialData().failed

        whenReady(result) { exp =>

          exp mustBe a[Exception]
          exp.getMessage mustBe exp.getMessage
        }
      }
    }
  }
}
