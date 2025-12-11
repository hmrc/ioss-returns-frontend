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

package models.etmp.intermediary

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.domain.Vrn

trait EtmpCustomerIdentification

object EtmpCustomerIdentification {

  implicit val reads: Reads[EtmpCustomerIdentification] =
    EtmpCustomerIdentificationNew.format.widen[EtmpCustomerIdentification] orElse
      EtmpCustomerIdentificationLegacy.format.widen[EtmpCustomerIdentification]

  implicit val writes: Writes[EtmpCustomerIdentification] = Writes {
    case ecin: EtmpCustomerIdentificationNew => Json.toJson(ecin)(EtmpCustomerIdentificationNew.format)
    case ecil: EtmpCustomerIdentificationLegacy => Json.toJson(ecil)(EtmpCustomerIdentificationLegacy.format)
  }
}

case class EtmpCustomerIdentificationLegacy(vrn: Vrn) extends EtmpCustomerIdentification

object EtmpCustomerIdentificationLegacy {

  implicit val format: OFormat[EtmpCustomerIdentificationLegacy] = Json.format[EtmpCustomerIdentificationLegacy]
}

case class EtmpCustomerIdentificationNew(idType: EtmpIdType, idValue: String) extends EtmpCustomerIdentification

object EtmpCustomerIdentificationNew {

  implicit val format: OFormat[EtmpCustomerIdentificationNew] = Json.format[EtmpCustomerIdentificationNew]
}

