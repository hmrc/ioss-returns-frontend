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

import logging.Logging
import models.*
import models.responses.*
import models.saveForLater.SavedUserAnswers
import play.api.http.Status.*
import play.api.libs.json.*
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object SaveForLaterHttpParser extends Logging {

  type SaveForLaterResponse = Either[ErrorResponse, Option[SavedUserAnswers]]
  type DeleteSaveForLaterResponse = Either[ErrorResponse, Boolean]
  type IntermediarySaveForLaterResponse = Either[ErrorResponse, Seq[SavedUserAnswers]]

  implicit object SaveForLaterReads extends HttpReads[SaveForLaterResponse] {
    override def read(method: String, url: String, response: HttpResponse): SaveForLaterResponse = {
      response.status match {
        case OK | CREATED =>
          response.json.validate[SavedUserAnswers] match {
            case JsSuccess(answers, _) =>
              Right(Some(answers))

            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn("Received NotFound for saved user answers")
          Right(None)

        case CONFLICT =>
          logger.warn("Received Conflict found for saved user answers")
          Left(ConflictFound)

        case status =>
          logger.warn("Received unexpected error from saved user answers")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  implicit object DeleteSaveForLaterReads extends HttpReads[DeleteSaveForLaterResponse] {
    override def read(method: String, url: String, response: HttpResponse): DeleteSaveForLaterResponse = {
      response.status match {
        case OK =>
          response.json.validate[Boolean] match {
            case JsSuccess(deleted, _) => Right(deleted)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case NOT_FOUND =>
          logger.warn("Received NotFound for saved user answers")
          Left(NotFound)
        case CONFLICT =>
          logger.warn("Received Conflict found for saved user answers")
          Left(ConflictFound)
        case status =>
          logger.warn("Received unexpected error from saved user answers")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response, status $status returned"))
      }
    }
  }

  implicit object IntermediarySaveForLaterReads extends HttpReads[IntermediarySaveForLaterResponse] {

    override def read(method: String, url: String, response: HttpResponse): IntermediarySaveForLaterResponse = {
      response.status match {
        case OK =>
          response.json.validate[Seq[SavedUserAnswers]] match {
            case JsSuccess(savedUserAnswers, _) => Right(savedUserAnswers)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse Intermediary saved user answers JSON $errors with " +
                s"response Json: ${response.json} and errors: $errors", errors)
              Left(InvalidJson)
          }

        case status =>
          logger.warn("Received unexpected error from Intermediary saved user answers")
          Left(UnexpectedResponseStatus(response.status, s"Unexpected response from Intermediary saved User Answers " +
            s"with status $status."))
      }
    }
  }
}
