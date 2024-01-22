/*
 * Copyright 2023 HM Revenue & Customs
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
import models.Period
import models.core.CoreVatReturn
import models.etmp.{EtmpObligations, EtmpVatReturn}
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpErrorFunctions, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatReturnConnector @Inject()(config: Configuration, httpClient: HttpClient)
                                  (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.ioss-returns")

  def getObligations(iossNumber: String)(implicit hc: HeaderCarrier): Future[EtmpObligations] =
    httpClient.GET[EtmpObligations](url = s"$baseUrl/obligations/$iossNumber")

  def submit(coreVatReturn: CoreVatReturn)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient.POST[CoreVatReturn, HttpResponse](
      s"$baseUrl/return",
      coreVatReturn
    )
  }

  def get(period: Period)(implicit hc: HeaderCarrier): Future[EtmpVatReturn] = {
    httpClient.GET[EtmpVatReturn](url = s"$baseUrl/return/$period")
  }
}
