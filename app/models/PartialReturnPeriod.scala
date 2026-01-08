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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.{LocalDate, Month}


case class PartialReturnPeriod(
                              firstDay: LocalDate,
                              lastDay: LocalDate,
                              year: Int,
                              month: Month
                              ) extends Period {

  override val isPartial: Boolean = true
}

object PartialReturnPeriod {
  val reads: Reads[PartialReturnPeriod] = {
    (
      (__ \ "firstDay").read[LocalDate] and
        (__ \ "lastDay").read[LocalDate] and
        (__ \ "year").read[Int] and
        (__ \ "month").read[String].map(m => Month.of(m.substring(1).toInt))
      )((firstDay, lastDay, year, month) => PartialReturnPeriod(firstDay, lastDay, year, month))
  }

  val writes: OWrites[PartialReturnPeriod] = {
    (
      (__ \ "firstDay").write[LocalDate] and
        (__ \ "lastDay").write[LocalDate] and
        (__ \ "year").write[Int] and
        (__ \ "month").write[String].contramap[Month](m => s"M${m.getValue}")
      )(partialReturnPeriod => Tuple.fromProductTyped(partialReturnPeriod))
  }

  implicit val format: Format[PartialReturnPeriod] = Format(reads, writes)
}
