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
import models.RegistrationWrapper
import models.enrolments.EACDEnrolments
import models.etmp.intermediary.IntermediaryRegistrationWrapper
import play.api.Configuration
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IntermediaryRegistrationConnector @Inject()(config: Configuration, httpClientV2: HttpClientV2)
                                                 (implicit ec: ExecutionContext) extends HttpErrorFunctions {

  private val baseUrl = config.get[Service]("microservice.services.ioss-intermediary-registration")

  def get(intermediaryNumber: String)(implicit hc: HeaderCarrier): Future[IntermediaryRegistrationWrapper] =
    httpClientV2.get(url"$baseUrl/get-registration/$intermediaryNumber").execute[IntermediaryRegistrationWrapper]

  def getAccounts()(implicit hc: HeaderCarrier): Future[EACDEnrolments] =
    httpClientV2.get(url"$baseUrl/accounts").execute[EACDEnrolments]

}