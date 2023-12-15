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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import queries.OptionalSalesAtVatRate

import java.time.LocalDate

final case class VatRateFromCountry(
                          rate: BigDecimal,
                          rateType: VatRateType,
                          validFrom: LocalDate,
                          validUntil: Option[LocalDate] = None,
                          salesAtVatRate: Option[OptionalSalesAtVatRate]
                        ) {

  lazy val rateForDisplay: String = if (rate.isWhole) {
    rate.toString.split('.').headOption.getOrElse(rate.toString) + "%"
  } else {
    rate.toString + "%"
  }

  lazy val asPercentage: BigDecimal =
    rate / 100
}

object VatRateFromCountry {

  val stringReads: Reads[VatRateFromCountry] = (
    (__ \ "rate").read[String].map(r => BigDecimal(r)) and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate] and
      (__ \ "salesAtVatRate").readNullable[OptionalSalesAtVatRate]
    ) (VatRateFromCountry.apply _)

  val decimalReads: Reads[VatRateFromCountry] = (
    (__ \ "rate").read[BigDecimal] and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate] and
      (__ \ "salesAtVatRate").readNullable[OptionalSalesAtVatRate]
    ) (VatRateFromCountry.apply _)

  implicit val reads: Reads[VatRateFromCountry] = decimalReads or stringReads

  implicit val writes: OWrites[VatRateFromCountry] = new OWrites[VatRateFromCountry] {

    override def writes(o: VatRateFromCountry): JsObject = {

      val validUntilJson = o.validUntil.map {
        v =>
          Json.obj("validUntil" -> Json.toJson(v))
      }.getOrElse(Json.obj())

      val salesAtVatRateJson = o.salesAtVatRate.map {
        v =>
          Json.obj("salesAtVatRate" -> Json.toJson(v))
      }.getOrElse(Json.obj())

      Json.obj(
        "rate" -> o.rate.toString,
        "rateType" -> Json.toJson(o.rateType),
        "validFrom" -> Json.toJson(o.validFrom)
      ) ++ validUntilJson ++ salesAtVatRateJson
    }
  }
}

sealed trait VatRateType

object VatRateType extends Enumerable.Implicits {

  case object Standard extends WithName("STANDARD") with VatRateType

  case object Reduced extends WithName("REDUCED") with VatRateType

  val values: Seq[VatRateType] = Seq(Standard, Reduced)

  implicit val enumerable: Enumerable[VatRateType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
