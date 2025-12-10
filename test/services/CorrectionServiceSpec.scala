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
import models.corrections.ReturnCorrectionValue
import models.etmp.{EtmpVatRateType, EtmpVatReturn, EtmpVatReturnGoodsSupplied}
import models.{Country, StandardPeriod, UnexpectedResponseStatus}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{BeforeAndAfterEach, PrivateMethodTester}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global


class CorrectionServiceSpec extends SpecBase with PrivateMethodTester with BeforeAndAfterEach {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  private val period1: StandardPeriod = period

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

        val returnCorrectionValue = ReturnCorrectionValue(400)

        val isPreviouslyDeclaredCountry: Boolean = true

        when(mockVatReturnConnector.getForIossNumber(eqTo(period1), eqTo(iossNumber))(any())) thenReturn Right(vatReturn1).toFuture
        when(mockVatReturnConnector.getReturnCorrectionValue(any(), any(), eqTo(period1))(any())) thenReturn returnCorrectionValue.toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val result = service.getAccumulativeVatForCountryTotalAmount(iossNumber, country1, period1).futureValue

        result `mustBe`(isPreviouslyDeclaredCountry, returnCorrectionValue.maximumCorrectionValue)
        verify(mockVatReturnConnector, times(1)).getForIossNumber(eqTo(period1), eqTo(iossNumber))(any())
      }

      "must throw an exception when there ia an error in trying to retrieve a vat return" in {

        val country: Country = arbitraryCountry.arbitrary.sample.value

        val returnCorrectionValue = ReturnCorrectionValue(0)

        when(mockVatReturnConnector.get(any())(any())) thenReturn Left(UnexpectedResponseStatus(NOT_FOUND, "error")).toFuture
        when(mockVatReturnConnector.getReturnCorrectionValue(any(), any(), eqTo(period1))(any())) thenReturn returnCorrectionValue.toFuture

        val service = new CorrectionService(mockVatReturnConnector)

        val result = service.getAccumulativeVatForCountryTotalAmount(iossNumber, country, period)

        whenReady(result.failed) { exp =>

          exp `mustBe` a[Exception]
        }
      }
    }
  }
}
