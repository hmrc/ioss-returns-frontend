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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits._
import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataConnector @Inject()(
                                        httpClientV2: HttpClientV2,
                                        config: Configuration
                                      )(implicit ec: ExecutionContext) extends Logging {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def getFinancialData(date: LocalDate)(implicit hc: HeaderCarrier): Future[FinancialData] = {
    httpClientV2.get(url"$baseUrl/financial-data/get/$date").execute[FinancialData]
  }

  def prepareFinancialData()(implicit hc: HeaderCarrier): Future[PrepareDataResponse] = {
    httpClientV2.get(url"$baseUrl/financial-data/prepare").execute[PrepareDataResponse]
  }

  def prepareFinancialDataWithIossNumber(iossNumber: String)(implicit hc: HeaderCarrier): Future[PrepareDataResponse] = {
    httpClientV2.get(url"$baseUrl/financial-data/prepare/$iossNumber").execute[PrepareDataResponse]
  }

  def getCharge(period: Period)(implicit hc: HeaderCarrier): Future[ChargeResponse] = {
    httpClientV2.get(url"$baseUrl/financial-data/charge/$period").execute[ChargeResponse]
  }

  def getChargeForIossNumber(period: Period, iossNumber: String)(implicit hc: HeaderCarrier): Future[ChargeResponse] = {
    httpClientV2.get(url"$baseUrl/financial-data/charge/$period/$iossNumber").execute[ChargeResponse]
  }

  def getCurrentPayments(iossNumber: String)(implicit hc: HeaderCarrier): Future[CurrentPaymentsResponse] = {
    httpClientV2.get(url"$baseUrl/financial-data/prepare").execute[CurrentPaymentsResponse]
  }
}