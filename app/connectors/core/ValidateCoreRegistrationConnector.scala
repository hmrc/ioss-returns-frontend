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

package connectors.core

import config.FrontendAppConfig
import connectors.core.ValidateCoreRegistrationHttpParser.{ValidateCoreRegistrationReads, ValidateCoreRegistrationResponse}
import logging.Logging
import models.core.{CoreRegistrationRequest, EisErrorResponse}
import models.responses.EisError
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpErrorFunctions, HttpException, StringContextOps}

import java.net.URL
import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidateCoreRegistrationConnector @Inject()(
                                                   frontendAppConfig: FrontendAppConfig,
                                                   httpClientV2: HttpClientV2,
                                                   clock: Clock
                                                 )(implicit ec: ExecutionContext) extends HttpErrorFunctions with Logging {

  private val baseUrl = frontendAppConfig.coreValidationService

  def validateCoreRegistration(
                                coreRegistrationRequest: CoreRegistrationRequest
                              )(implicit hc: HeaderCarrier): Future[ValidateCoreRegistrationResponse] = {

    val url: URL = url"$baseUrl/validate-core-registration"

    httpClientV2.post(url).withBody(Json.toJson(coreRegistrationRequest)).execute[ValidateCoreRegistrationResponse].recover {
      case e: HttpException =>
        logger.error(
          s"Unexpected error response from backend"
        )
        Left(EisError(
          EisErrorResponse(Instant.now(clock), s"UNEXPECTED_${e.responseCode.toString}", e.message)
        ))

      case e =>
        logger.error(
          s"Unexpected error response from backend"
        )
        Left(EisError(
          EisErrorResponse(Instant.now(clock), "UNEXPECTED", e.getMessage)
        ))
    }
  }

}
