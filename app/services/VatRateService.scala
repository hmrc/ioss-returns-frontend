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

import connectors.EuVatRateConnector
import logging.Logging
import models.{Country, Period, VatRateFromCountry}
import queries.SalesToCountryWithOptionalSales
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode


class VatRateService @Inject()(
                                euVatRateConnector: EuVatRateConnector
                              )(implicit ec: ExecutionContext) extends Logging {

  def vatRates(period: Period, country: Country)(implicit hc: HeaderCarrier): Future[Seq[VatRateFromCountry]] = {
    euVatRateConnector
      .getEuVatRates(country, period.firstDay, period.lastDay)
      .map(_
        .map(VatRateFromCountry.fromEuVatRate)
        .filterNot(_.rate == BigDecimal(0))
      )
  }

  def getRemainingVatRatesForCountry(
                                      period: Period,
                                      country: Country,
                                      currentVatRatesForCountry: SalesToCountryWithOptionalSales
                                    )(implicit hc: HeaderCarrier): Future[Seq[VatRateFromCountry]] = {
    vatRates(period, country).map(_.filterNot { vatRateForCountry =>
        currentVatRatesForCountry.vatRatesFromCountry.exists(_.map(_.rate).contains(vatRateForCountry.rate))
      }
    )
  }

  def standardVatOnSales(netSales: BigDecimal, vatRate: VatRateFromCountry): BigDecimal =
    ((netSales * vatRate.rate) / 100).setScale(2, RoundingMode.HALF_EVEN)
}