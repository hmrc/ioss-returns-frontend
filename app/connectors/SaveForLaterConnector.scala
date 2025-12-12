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

package connectors

import config.Service
import connectors.SaveForLaterHttpParser.*
import models.Period
import models.requests.SaveForLaterRequest
import play.api.Configuration
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  private val url: URL = url"$baseUrl/save-for-later"
  private val intermediaryUrl: URL = url"$baseUrl/intermediary-save-for-later"

  def submit(s4lRequest: SaveForLaterRequest)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    httpClientV2.post(url).withBody(Json.toJson(s4lRequest)).execute[SaveForLaterResponse]
  }

  def get()(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    httpClientV2.get(url).execute[SaveForLaterResponse]
  }

  def submitForIntermediary(s4lRequest: SaveForLaterRequest)(implicit hc: HeaderCarrier): Future[SaveForLaterResponse] = {
    httpClientV2.post(intermediaryUrl).withBody(Json.toJson(s4lRequest)).execute[SaveForLaterResponse]
  }

  def delete(period: Period)(implicit hc: HeaderCarrier): Future[DeleteSaveForLaterResponse] = {
    val url: URL = url"$baseUrl/save-for-later/delete/$period"

    httpClientV2.get(url).execute[DeleteSaveForLaterResponse]
  }
}
