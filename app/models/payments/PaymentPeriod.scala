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

package models.payments

import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.{LocalDate, Month}

final case class PaymentPeriod(year: Int, month: Month, dueDate: LocalDate)


object PaymentPeriod {

  implicit val reads: Reads[PaymentPeriod] = {
    (
      (__ \ "year").read[Int] and
        (__ \ "month").read[Int].map(Month.of) and
        (__ \ "dueDate").read[LocalDate]
      )(PaymentPeriod.apply _)
  }


  implicit val writes: Writes[PaymentPeriod] = {
    (
      (__ \ "year").write[Int] and
        (__ \ "month").write[Int].contramap[Month](_.getValue) and
        (__ \ "dueDate").write[LocalDate]
      )(paymentPeriod => Tuple.fromProductTyped(paymentPeriod))

  }

  implicit val format: Format[PaymentPeriod] = Format(reads, writes)
}