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

import logging.Logging
import models.{ErrorResponse, InvalidJson, NotFound, UnexpectedResponseStatus}
import models.external.ExternalEntryUrl
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object ExternalEntryUrlHttpParser extends Logging {

  type ExternalEntryUrlResponse = Either[ErrorResponse, ExternalEntryUrl]

  implicit object ExternalEntryUrlResponseReads extends HttpReads[ExternalEntryUrlResponse] {
    override def read(method: String, url: String, response: HttpResponse): ExternalEntryUrlResponse =
      response.status match {
        case OK =>
          response.json.validate[ExternalEntryUrl] match {
            case JsSuccess(value, _) =>
              Right(value)
            case JsError(errors) =>
              logger.error(s"Could not read payload as a ExternalEntryUrlResponse model $errors", errors)
              Left(InvalidJson)
        }

        case NOT_FOUND =>
          Left(NotFound)

        case status =>
          Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status with body ${response.body}"))
      }
  }
}
