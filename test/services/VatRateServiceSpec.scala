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

package services

import base.SpecBase
import connectors.EuVatRateConnector
import models.{Country, EuVatRate, VatRateFromCountry}
import models.VatRateType.{Reduced, Standard}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import queries.{SalesToCountryWithOptionalSales, VatRateWithOptionalSalesFromCountry}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global

class VatRateServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val country = arbitrary[Country].sample.value

  private implicit lazy val emptyHC: HeaderCarrier = HeaderCarrier()

  private val mockEuVatRateConnector: EuVatRateConnector = mock[EuVatRateConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockEuVatRateConnector)
  }

  "VatRate Service" - {

    ".vatRates" - {

      "covert all vat rates return to VatRateFromCountry" - {

        "and which have no end date and removes 0% rate" in {

          val rates: Seq[EuVatRate] = Seq(
            EuVatRate(country, BigDecimal(0), Standard, period.firstDay.minusDays(1)),
            EuVatRate(country, BigDecimal(1), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(2), Reduced, period.lastDay),
            EuVatRate(country, BigDecimal(3), Reduced, period.lastDay.plusDays(1))
          )

          when(mockEuVatRateConnector.getEuVatRates(any(), any(), any())(any())) thenReturn rates.toFuture

          val service = new VatRateService(mockEuVatRateConnector)

          val result = service.vatRates(period, country).futureValue

          result must contain theSameElementsAs Seq(
            VatRateFromCountry(BigDecimal(1), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(2), Reduced, period.lastDay),
            VatRateFromCountry(BigDecimal(3), Reduced, period.lastDay.plusDays(1))
          )
        }

      }
    }

    ".getRemainingVatRatesForCountry" - {

      "must return the remaining VAT rate(s) for a given country" - {

        "when there is only one VAT rate remaining" in {

          val rates: Seq[EuVatRate] = Seq(
            EuVatRate(country, BigDecimal(21.0), Standard, period.firstDay),
            EuVatRate(country, BigDecimal(17.0), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(14.0), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(5.0), Reduced, period.firstDay)
          )

          val currentlyAnsweredVatRates: SalesToCountryWithOptionalSales = SalesToCountryWithOptionalSales(
            country = country,
            vatRatesFromCountry = Some(
              List(
                VatRateWithOptionalSalesFromCountry(BigDecimal(21.0), Standard, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(14.0), Reduced, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(5.0), Reduced, period.firstDay, None, None)
              )
            )
          )

          when(mockEuVatRateConnector.getEuVatRates(any(), any(), any())(any())) thenReturn rates.toFuture

          val service = new VatRateService(mockEuVatRateConnector)

          val result = service.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates).futureValue

          result must contain theSameElementsAs Seq(VatRateFromCountry(BigDecimal(17.0), Reduced, period.firstDay))
        }

        "when there are multiple VAT rates remaining" in {

          val rates: Seq[EuVatRate] = Seq(
            EuVatRate(country, BigDecimal(26.0), Standard, period.firstDay),
            EuVatRate(country, BigDecimal(21.0), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(13.0), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(7.5), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(4.3), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(2.09), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(0.3), Reduced, period.firstDay)
          )

          val currentlyAnsweredVatRates: SalesToCountryWithOptionalSales = SalesToCountryWithOptionalSales(
            country = country,
            vatRatesFromCountry = Some(
              List(
                VatRateWithOptionalSalesFromCountry(BigDecimal(13.0), Reduced, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(4.3), Reduced, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(2.09), Reduced, period.firstDay, None, None),
              )
            )
          )

          when(mockEuVatRateConnector.getEuVatRates(any(), any(), any())(any())) thenReturn rates.toFuture

          val service = new VatRateService(mockEuVatRateConnector)

          val result = service.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates).futureValue

          result must contain theSameElementsAs Seq(
            VatRateFromCountry(BigDecimal(26.0), Standard, period.firstDay),
            VatRateFromCountry(BigDecimal(21.0), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(7.5), Reduced, period.firstDay),
            VatRateFromCountry(BigDecimal(0.3), Reduced, period.firstDay)
          )
        }

        "when there are no VAT rates remaining" in {

          val rates: Seq[EuVatRate] = Seq(
            EuVatRate(country, BigDecimal(21.7), Standard, period.firstDay),
            EuVatRate(country, BigDecimal(12.0), Reduced, period.firstDay),
            EuVatRate(country, BigDecimal(9.4), Reduced, period.firstDay)
          )

          val currentlyAnsweredVatRates: SalesToCountryWithOptionalSales = SalesToCountryWithOptionalSales(
            country = country,
            vatRatesFromCountry = Some(
              List(
                VatRateWithOptionalSalesFromCountry(BigDecimal(21.7), Standard, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(12.0), Standard, period.firstDay, None, None),
                VatRateWithOptionalSalesFromCountry(BigDecimal(9.4), Reduced, period.firstDay, None, None),
              )
            )
          )

          when(mockEuVatRateConnector.getEuVatRates(any(), any(), any())(any())) thenReturn rates.toFuture

          val service = new VatRateService(mockEuVatRateConnector)

          val result = service.getRemainingVatRatesForCountry(period, country, currentlyAnsweredVatRates).futureValue

          result mustBe Seq.empty
        }
      }
    }
  }
}
