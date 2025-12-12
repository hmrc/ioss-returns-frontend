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
import models.etmp.EtmpVatReturn
import models.{ErrorResponse, InvalidJson, UnexpectedResponseStatus}
import play.api.http.Status.OK
import play.api.libs.json.{JsError, JsSuccess}
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object VatReturnHttpParser extends Logging {

  type EtmpVatReturnResponse = Either[ErrorResponse, EtmpVatReturn]

  implicit object EtmpVatReturnReads extends HttpReads[EtmpVatReturnResponse] {

    override def read(method: String, url: String, response: HttpResponse): EtmpVatReturnResponse = {
      response.status match {
        case OK =>
          response.json.validate[EtmpVatReturn] match {
            case JsSuccess(etmpVatReturn, _) => Right(etmpVatReturn)
            case JsError(errors) =>
              logger.warn(s"Failed trying to parse JSON $errors. Json was ${response.json}", errors)
              Left(InvalidJson)
          }
        case _ =>
          logger.warn("Failed to retrieve vat return")
          Left(UnexpectedResponseStatus(response.status, response.body))
      }
    }
  }
}
