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

package connectors

import config.Service
import connectors.FinancialDataHttpParser.{ChargeReads, ChargeResponse}
import connectors.PrepareDataHttpParser.{PrepareDataReads, PrepareDataResponse}
import logging.Logging
import models.Period
import models.financialdata.CurrentPaymentsHttpParser.CurrentPaymentsResponse
import models.financialdata.FinancialData
import models.financialdata.FinancialData._
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject()(
                                        http: HttpClient,
                                        config: Configuration
                                      )(implicit ec: ExecutionContext) extends Logging {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def getFinancialData(date: LocalDate)(implicit hc: HeaderCarrier): Future[FinancialData] = {
    val url = s"$baseUrl/financial-data/get/$date"
    http.GET[FinancialData](url)
  }

  def prepareFinancialData()(implicit hc: HeaderCarrier): Future[PrepareDataResponse] = {
    val url = s"$baseUrl/financial-data/prepare"
    http.GET[PrepareDataResponse](url)
  }

  def getCharge(period: Period)(implicit hc: HeaderCarrier): Future[ChargeResponse] = {
    val url = s"$baseUrl/financial-data/charge/$period"
    http.GET[ChargeResponse](url)
  }

  def getChargeForIossNumber(period: Period, iossNumber: String)(implicit hc: HeaderCarrier): Future[ChargeResponse] = {
    val url = s"$baseUrl/financial-data/charge/$period/$iossNumber"
    http.GET[ChargeResponse](url)
  }

  def getCurrentPayments(iossNumber: String)(implicit hc: HeaderCarrier): Future[CurrentPaymentsResponse] = {
    val url = s"$baseUrl/financial-data/prepare"
    http.GET[CurrentPaymentsResponse](url)
  }
}