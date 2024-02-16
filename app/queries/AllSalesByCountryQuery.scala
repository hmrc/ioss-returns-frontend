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

package queries

import models.{Country, Index, VatOnSales, VatRateFromCountry, VatRateType}
import pages.PageConstants
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class OptionalSalesAtVatRate(
                                   netValueOfSales: Option[BigDecimal],
                                   vatOnSales: Option[VatOnSales]
                                 )

object OptionalSalesAtVatRate {

  implicit val format: OFormat[OptionalSalesAtVatRate] = Json.format[OptionalSalesAtVatRate]
}

case class VatRateWithOptionalSalesFromCountry(
                                                rate: BigDecimal,
                                                rateType: VatRateType,
                                                validFrom: LocalDate,
                                                validUntil: Option[LocalDate] = None,
                                                salesAtVatRate: Option[OptionalSalesAtVatRate]
                                              )

object VatRateWithOptionalSalesFromCountry {

  val stringReads: Reads[VatRateWithOptionalSalesFromCountry] = (
    (__ \ "rate").read[String].map(r => BigDecimal(r)) and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate] and
      (__ \ "salesAtVatRate").readNullable[OptionalSalesAtVatRate]
    )(VatRateWithOptionalSalesFromCountry.apply _)

  val decimalReads: Reads[VatRateWithOptionalSalesFromCountry] = (
    (__ \ "rate").read[BigDecimal] and
      (__ \ "rateType").read[VatRateType] and
      (__ \ "validFrom").read[LocalDate] and
      (__ \ "validUntil").readNullable[LocalDate] and
      (__ \ "salesAtVatRate").readNullable[OptionalSalesAtVatRate]
    )(VatRateWithOptionalSalesFromCountry.apply _)

  implicit val reads: Reads[VatRateWithOptionalSalesFromCountry] = decimalReads or stringReads

  implicit val writes: OWrites[VatRateWithOptionalSalesFromCountry] = new OWrites[VatRateWithOptionalSalesFromCountry] {

    override def writes(o: VatRateWithOptionalSalesFromCountry): JsObject = {

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
      ) ++
        validUntilJson ++
        salesAtVatRateJson
    }
  }

  def fromVatRateFromCountry(vatRateFromCountry: VatRateFromCountry): VatRateWithOptionalSalesFromCountry = {
    VatRateWithOptionalSalesFromCountry(
      vatRateFromCountry.rate,
      vatRateFromCountry.rateType,
      vatRateFromCountry.validFrom,
      vatRateFromCountry.validUntil,
      None
    )
  }
}

case class SalesToCountryWithOptionalSales(
                                            country: Country,
                                            vatRatesFromCountry: Option[List[VatRateWithOptionalSalesFromCountry]]
                                          )

object SalesToCountryWithOptionalSales {

  implicit val format: OFormat[SalesToCountryWithOptionalSales] = Json.format[SalesToCountryWithOptionalSales]
}


case class AllSalesByCountryQuery(countryIndex: Index) extends Gettable[SalesToCountryWithOptionalSales] with Settable[SalesToCountryWithOptionalSales] {

  override def path: JsPath = JsPath \ PageConstants.sales \ countryIndex.position
}
