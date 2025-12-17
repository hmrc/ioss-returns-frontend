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

package connectors.core

import logging.Logging
import models.core.{CoreRegistrationValidationResult, EisErrorResponse}
import models.responses.{EisError, ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import java.time.Instant
import java.util.UUID

object ValidateCoreRegistrationHttpParser extends Logging {

  type ValidateCoreRegistrationResponse = Either[ErrorResponse, CoreRegistrationValidationResult]

  implicit object ValidateCoreRegistrationReads extends HttpReads[ValidateCoreRegistrationResponse] {
    override def read(method: String, url: String, response: HttpResponse): ValidateCoreRegistrationResponse = {
      response.status match {
        case OK => response.json.validate[CoreRegistrationValidationResult] match {
          case JsSuccess(validateCoreRegistration, _) => Right(validateCoreRegistration)
          case JsError(errors) =>
            logger.error(s"Failed trying to parse JSON $errors. JSON was ${response.json}")
            Left(InvalidJson)
        }

        case status =>
          logger.info(s"Response received from EIS ${response.status} with body ${response.body}")
          if (response.body.isEmpty) {
            val uuid = UUID.randomUUID()
            logger.error(s"Response received from EIS ${response.status} with empty body and self-generated correlationId $uuid")
            Left(
              EisError(
                EisErrorResponse(Instant.now(), status.toString, "The response body was empty")
              ))
          } else {
            response.json.validateOpt[EisErrorResponse] match {
              case JsSuccess(Some(eisErrorResponse), _) =>
                logger.error(s"There was an error from EIS when submitting a validation with status $status and $eisErrorResponse")
                Left(EisError(eisErrorResponse))

              case _ =>
                logger.error(s"Received UnexpectedResponseStatus with status code $status with body ${response.body}")
                Left(UnexpectedResponseStatus(status, s"Received unexpected response code $status"))
            }
          }
      }
    }
  }

}
