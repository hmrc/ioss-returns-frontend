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

package models.enrolments

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case class EACDEnrolment(service: String, state: String, activationDate: Option[LocalDateTime], identifiers: Seq[EACDIdentifiers])

object EACDEnrolment {
  private val dateTimeEACDFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

  val reads: Reads[EACDEnrolment] = {
    (
      (__ \ "service").read[String] and
        (__ \ "state").read[String] and
        (__ \ "activationDate").readNullable[String].map(_.map(t => LocalDateTime.parse(t, dateTimeEACDFormat))) and
        (__ \ "identifiers").read[Seq[EACDIdentifiers]]
      )((service, state, activationDate, identifiers) => EACDEnrolment(service, state, activationDate, identifiers))
  }

  val writes: OWrites[EACDEnrolment] = {
    (
      (__ \ "service").write[String] and
        (__ \ "state").write[String] and
        (__ \ "activationDate").writeNullable[String].contramap[Option[LocalDateTime]](_.map(t => t.format(dateTimeEACDFormat))) and
        (__ \ "identifiers").write[Seq[EACDIdentifiers]]
      )(eACDEnrolment => Tuple.fromProductTyped(eACDEnrolment))
  }

  implicit val format: OFormat[EACDEnrolment] = OFormat(reads, writes)
}
