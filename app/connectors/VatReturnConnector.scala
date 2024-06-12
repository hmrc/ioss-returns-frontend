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
import connectors.ExternalEntryUrlHttpParser._
import connectors.VatReturnHttpParser.{EtmpVatReturnReads, EtmpVatReturnResponse}
import models.Period
import models.core.CoreVatReturn
import models.corrections.ReturnCorrectionValue
import models.etmp.EtmpObligations
import play.api.Configuration
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                  (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def getObligations(iossNumber: String)(implicit hc: HeaderCarrier): Future[EtmpObligations] =
    httpClientV2.get(url"$baseUrl/obligations/$iossNumber").execute[EtmpObligations]

  def submit(coreVatReturn: CoreVatReturn)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    httpClientV2.post(url"$baseUrl/return").withBody(Json.toJson(coreVatReturn)).execute[HttpResponse]

  def get(period: Period)(implicit hc: HeaderCarrier): Future[EtmpVatReturnResponse] =
    httpClientV2.get(url"$baseUrl/return/$period").execute[EtmpVatReturnResponse]

  def getForIossNumber(period: Period, iossNumber: String)(implicit hc: HeaderCarrier): Future[EtmpVatReturnResponse] =
    httpClientV2.get(url"$baseUrl/return/$period/$iossNumber").execute[EtmpVatReturnResponse]

  def getSavedExternalEntry()(implicit hc: HeaderCarrier): Future[ExternalEntryUrlResponse] =
    httpClientV2.get(url"$baseUrl/external-entry").execute[ExternalEntryUrlResponse]

  def getReturnCorrectionValue(iossNumber: String, countryCode: String, period: Period)(implicit hc: HeaderCarrier): Future[ReturnCorrectionValue] =
    httpClientV2.get(url"$baseUrl/max-correction-value/$iossNumber/$countryCode/$period").execute[ReturnCorrectionValue]
}
