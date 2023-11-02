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

package models.etmp

import play.api.libs.json.{Json, OFormat, Reads, Writes}

sealed trait TraderId

object TraderId {

  implicit val reads: Reads[TraderId] =
    VatNumberTraderId.format.widen[TraderId] orElse
      TaxRefTraderID.format.widen[TraderId]

  implicit val writes: Writes[TraderId] = Writes {
    case v: VatNumberTraderId => Json.toJson(v)(VatNumberTraderId.format)
    case tr: TaxRefTraderID => Json.toJson(tr)(TaxRefTraderID.format)
  }
}

final case class VatNumberTraderId(vatNumber: String) extends TraderId

object VatNumberTraderId {

  implicit val format: OFormat[VatNumberTraderId] = Json.format[VatNumberTraderId]
}

final case class TaxRefTraderID(taxReferenceNumber: String) extends TraderId

object TaxRefTraderID {

  implicit val format: OFormat[TaxRefTraderID] = Json.format[TaxRefTraderID]
}
