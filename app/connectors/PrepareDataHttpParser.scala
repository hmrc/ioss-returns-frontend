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
import models.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import models.payments.PrepareData

object PrepareDataHttpParser extends Logging {

  type PrepareDataResponse = Either[ErrorResponse, PrepareData]

  implicit object PrepareDataReads extends HttpReads[PrepareDataResponse] {

    override def read(method: String, url: String, response: HttpResponse): PrepareDataResponse = {
      response.status match {
        case OK =>
          response.json.validate[PrepareData] match {
            case JsSuccess(response, _) =>
              logger.info(s"Response was good with $response")
              Right(response)
            case JsError(errors) =>
              logger.error(s"Failed trying to parse prepared financial data JSON $errors. Json was ${response.json}")
              Left(InvalidJson)
          }
        case _ =>
          logger.error("Failed to retrieve prepared financial data")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }
}