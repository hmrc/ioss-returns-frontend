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
import connectors.VatReturnConnector
import models.etmp.{EtmpVatRateType, EtmpVatReturn, EtmpVatReturnCorrection, EtmpVatReturnGoodsSupplied}
import models.{Country, Period, StandardPeriod, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CorrectionServiceSpec extends SpecBase with PrivateMethodTester with BeforeAndAfterEach {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  private val period1: StandardPeriod = period
  private val period2: Period = period1.getNext
  private val period3: Period = period2.getNext
  private val period4: Period = period3.getNext

  private val vatReturn: EtmpVatReturn = arbitraryEtmpVatReturn.arbitrary.sample.value

  override def beforeEach(): Unit = {
    Mockito.reset(mockVatReturnConnector)
  }

  "CorrectionService" - {

    ".getAccumulativeVatForCountryTotalAmount" - {

      "must return the vatAmountGBP sum of all vatReturns within period range for the selected correction country" in {

        val country1: Country = Country("DE", "Germany")
        val country2: Country = Country("FR", "France")

        val vatReturn1 = vatReturn.copy(
          periodKey = period1.toEtmpPeriodString,
          goodsSupplied = Seq(
            EtmpVatReturnGoodsSupplied(
              msOfConsumption = country1.code,
              vatAmountGBP = BigDecimal(500),
              vatRateType = EtmpVatRateType.StandardVatRate,
              taxableAmountGBP = BigDecimal(2500)
          ),
            EtmpVatReturnGoodsSupplied(
              msOfConsumption = country2.code,
              vatAmountGBP = BigDecimal(800),
              vatRateType = EtmpVatRateType.StandardVatRate,
              taxableAmountGBP = BigDecimal(4000)
            )
          ),
          correctionPreviousVATReturn = Seq.empty
        )

        val vatReturn2WithCorrection = vatReturn1.copy(
          periodKey = period2.toEtmpPeriodString,
          goodsSupplied = Seq.empty,
          correctionPreviousVATReturn = Seq(
            EtmpVatReturnCorrection(
              periodKey = period1.toEtmpPeriodString,
              msOfConsumption = country1.code,
              totalVATAmountCorrectionGBP = BigDecimal(-100),
              periodFrom = period1.toEtmpPeriodString,
              periodTo = period2.toEtmpPeriodString,
              totalVATAmountCorrectionEUR = BigDecimal(-100)
            ),
            EtmpVatReturnCorrection(
              periodKey = period1.toEtmpPeriodString,
              msOfConsumption = country2.code,
              totalVATAmountCorrectionGBP = BigDecimal(-400),
              periodFrom = period1.toEtmpPeriodString,
              periodTo = period2.toEtmpPeriodString,
              totalVATAmountCorrectionEUR = BigDecimal(-400)
            )
          )
        )

        val vatReturn3WithCorrection = vatReturn1.copy(
          periodKey = period3.toEtmpPeriodString,
          goodsSupplied = Seq(
            EtmpVatReturnGoodsSupplied(
              msOfConsumption = country2.code,
              vatAmountGBP = BigDecimal(300),
              vatRateType = EtmpVatRateType.StandardVatRate,
              taxableAmountGBP = BigDecimal(1500)
            ),
          ),
          correctionPreviousVATReturn = Seq(
            EtmpVatReturnCorrection(
              periodKey = period1.toEtmpPeriodString,
              msOfConsumption = country1.code,
              totalVATAmountCorrectionGBP = BigDecimal(-50),
              periodFrom = period1.toEtmpPeriodString,
              periodTo = period3.toEtmpPeriodString,
              totalVATAmountCorrectionEUR = BigDecimal(-50)
            )
          )
        )

        val accumulativeVatTotalAmount: BigDecimal = BigDecimal(350)
        val isPreviouslyDeclaredCountry: Boolean = true

        val periodFrom: Period = period1
        val periodTo: Period = period4

        when(mockVatReturnConnector.get(eqTo(period1))(any())) thenReturn Right(vatReturn1).toFuture
        when(mockVatReturnConnector.get(eqTo(period2))(any())) thenReturn Right(vatReturn2WithCorrection).toFuture
        when(mockVatReturnConnector.get(eqTo(period3))(any())) thenReturn Right(vatReturn3WithCorrection).toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val result = service.getAccumulativeVatForCountryTotalAmount(periodFrom, periodTo, country1).futureValue

        result mustBe (isPreviouslyDeclaredCountry, accumulativeVatTotalAmount)
        verify(mockVatReturnConnector, times(1)).get(eqTo(period1))(any())
        verify(mockVatReturnConnector, times(1)).get(eqTo(period2))(any())
        verify(mockVatReturnConnector, times(1)).get(eqTo(period3))(any())
      }

      "must throw an exception when there ia an error in trying to retrieve a vat return from getAllReturnsInPeriodRange" in {

        val country: Country = arbitraryCountry.arbitrary.sample.value

        val periodFrom: Period = period1
        val periodTo: Period = period2

        when(mockVatReturnConnector.get(any())(any())) thenReturn Left(UnexpectedResponseStatus(NOT_FOUND, "error")).toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val result = service.getAccumulativeVatForCountryTotalAmount(periodFrom, periodTo, country)

        whenReady(result.failed) { exp =>

          exp mustBe a[Exception]
        }
      }
    }

    ".getAllReturnsInPeriodRange" - {

      "must return all vat returns within period range when server returns ETMP vat returns payload" in {

        val periodFrom: Period = period1
        val periodTo: Period = period3

        when(mockVatReturnConnector.get(any())(any())) thenReturn Right(vatReturn).toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val getAllReturnsInPeriodRange = PrivateMethod[Future[Seq[EtmpVatReturn]]](Symbol("getAllReturnsInPeriodRange"))

        val result = service invokePrivate getAllReturnsInPeriodRange(periodFrom, periodTo, hc)

        result.futureValue mustBe Seq(vatReturn, vatReturn)
        verify(mockVatReturnConnector, times(1)).get(eqTo(period1))(any())
        verify(mockVatReturnConnector, times(1)).get(eqTo(period2))(any())
      }

      "must return an exception when connector returns a Left(error)" in {

        val errorMessage: String = "error"

        val periodFrom: Period = period1
        val periodTo: Period = period2

        when(mockVatReturnConnector.get(any())(any())) thenReturn Left(UnexpectedResponseStatus(NOT_FOUND, errorMessage)).toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val getAllReturnsInPeriodRange = PrivateMethod[Future[Seq[EtmpVatReturn]]](Symbol("getAllReturnsInPeriodRange"))

        val result = service invokePrivate getAllReturnsInPeriodRange(periodFrom, periodTo, hc)

        whenReady(result.failed) { exp =>

          exp mustBe a[Exception]
          exp.getMessage mustBe s"Error when trying to retrieve vat return from getAllPeriods with error: $errorMessage"
        }
      }
    }

    ".getAllPeriods" - {

      "must return an empty sequence if no periods within the match range are found" in {

        val periodFrom: Period = period1
        val periodTo: Period = period1

        val service = new CorrectionService(mockVatReturnConnector)

        val getAllPeriods = PrivateMethod[Seq[Period]](Symbol("getAllPeriods"))

        val result = service invokePrivate getAllPeriods(periodFrom, periodTo)

        result mustBe Seq.empty
      }

      "must return a sequence of multiple periods when multiple periods exists within the match range" in {

        val periodFrom: Period = period1
        val periodTo: Period = period4

        val service = new CorrectionService(mockVatReturnConnector)

        val getAllPeriods = PrivateMethod[Seq[Period]](Symbol("getAllPeriods"))

        val result = service invokePrivate getAllPeriods(periodFrom, periodTo)

        result mustBe Seq(period1, period2, period3)
        result mustNot contain theSameElementsAs Seq(period4)
      }

      "must only return a single period when range difference is one period" in {

        val periodFrom: Period = period1
        val periodTo: Period = period2

        val service = new CorrectionService(mockVatReturnConnector)

        val getAllPeriods = PrivateMethod[Seq[Period]](Symbol("getAllPeriods"))

        val result = service invokePrivate getAllPeriods(periodFrom, periodTo)

        result mustBe Seq(period1)
        result mustNot contain theSameElementsAs Seq(period2)
      }
    }
  }
}
