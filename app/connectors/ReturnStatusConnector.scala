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
import connectors.CurrentReturnHttpParser._
import connectors.ReturnStatusesHttpParser._
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions}

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnStatusConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                     (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")
  private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    .withLocale(Locale.UK)
    .withZone(ZoneId.systemDefault())

  def getCurrentReturns(iossNumber: String)(implicit hc: HeaderCarrier): Future[CurrentReturnsResponse] =
    httpClient.GET[CurrentReturnsResponse](url = s"$baseUrl/vat-returns/current-returns/$iossNumber")

  def listStatuses(commencementDate: LocalDate)(implicit hc: HeaderCarrier): Future[ReturnStatusesResponse] = {

    val url = s"$baseUrl/vat-returns/statuses/${dateTimeFormatter.format(commencementDate)}"

    httpClient.GET[ReturnStatusesResponse](url)
  }
}
