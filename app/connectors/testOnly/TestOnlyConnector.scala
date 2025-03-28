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
import models.Period
import models.external.ExternalRequest
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyConnector @Inject()(
                               config: Configuration,
                               httpClientV2: HttpClientV2
                             )(implicit ec: ExecutionContext) {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def externalEntry(externalRequest: ExternalRequest, endpointName: String, maybePeriod: Option[Period], maybeLang: Option[String])
                   (implicit hc: HeaderCarrier): Future[ExternalResponseResponse] = {
    val url: URL =
      (maybePeriod, maybeLang) match {
        case (Some(period), Some(lang)) =>
          url"$baseUrl/external-entry/$endpointName?period=$period&lang=$lang"
        case (Some(period), None) =>
          url"$baseUrl/external-entry/$endpointName?period=$period"
        case (None, Some(lang)) =>
          url"$baseUrl/external-entry/$endpointName?lang=$lang"
        case _ =>
          url"$baseUrl/external-entry/$endpointName"
      }
    httpClientV2.post(url).withBody(Json.toJson(externalRequest)).execute[ExternalResponseResponse]
  }
}