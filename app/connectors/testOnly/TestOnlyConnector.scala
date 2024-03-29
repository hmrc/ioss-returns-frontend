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

package connectors.testOnly

import config.Service
import connectors.testOnly.TestOnlyExternalResponseHttpParser.{ExternalResponseReads, ExternalResponseResponse}
import models.external.ExternalRequest
import models.Period
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyConnector @Inject()(
                               config: Configuration,
                               httpClient: HttpClient
                             )(implicit ec: ExecutionContext) {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def externalEntry(externalRequest: ExternalRequest, endpointName: String, maybePeriod: Option[Period], maybeLang: Option[String])
                   (implicit hc: HeaderCarrier): Future[ExternalResponseResponse] = {
    val url =
      (maybePeriod, maybeLang) match {
        case (Some(period), Some(lang)) =>
          s"$baseUrl/external-entry/$endpointName?period=$period&lang=$lang"
        case (Some(period), None) =>
          s"$baseUrl/external-entry/$endpointName?period=$period"
        case (None, Some(lang)) =>
          s"$baseUrl/external-entry/$endpointName?lang=$lang"
        case _ =>
          s"$baseUrl/external-entry/$endpointName"
      }
    httpClient.POST[ExternalRequest, ExternalResponseResponse](url, externalRequest)
  }
}