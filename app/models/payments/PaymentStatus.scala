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

package models.payments

import models.json.WritesString
import play.api.libs.json.{JsPath, Reads, Writes}

sealed trait PaymentStatus

object PaymentStatus {

  case object Unpaid extends PaymentStatus

  case object Partial extends PaymentStatus

  case object Unknown extends PaymentStatus

  implicit val reads: Reads[PaymentStatus] = JsPath
    .read[String]
    .map(_.toUpperCase)
    .map {
      case "UNPAID" => Unpaid
      case "PARTIAL" => Partial
      case "UNKNOWN" => Unknown
    }
  implicit val writes: Writes[PaymentStatus] = WritesString[PaymentStatus] {
    case Unpaid => "UNPAID"
    case Partial => "PARTIAL"
    case Unknown => "UNKNOWN"
  }
}