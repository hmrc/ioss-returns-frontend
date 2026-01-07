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

package models

import config.Constants.ukCountryCodeAreaPrefix
import models.etmp.intermediary.{EtmpCustomerIdentificationLegacy, EtmpCustomerIdentificationNew, EtmpIdType}
import models.etmp.{EtmpDisplayRegistration, VatCustomerInfo}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.domain.Vrn


case class RegistrationWrapper(vatInfo: Option[VatCustomerInfo], registration: EtmpDisplayRegistration) {
  
  val maybeVrn: Option[Vrn] = {
    registration.customerIdentification match
      case EtmpCustomerIdentificationLegacy(vrn) => Some(vrn)
      case EtmpCustomerIdentificationNew(idType, idValue) => 
        if (idType == EtmpIdType.VRN) Some(Vrn(idValue)) else None
      case _ => None
  }
  
  def getCompanyName(): String = {
    val clientCompanyName: String = vatInfo match {
        case Some(nonOptionalVatInfo) if nonOptionalVatInfo.desAddress.countryCode.startsWith(ukCountryCodeAreaPrefix) =>
          nonOptionalVatInfo.organisationName
            .orElse(nonOptionalVatInfo.individualName)    
            .getOrElse(throw new IllegalStateException("Unable to retrieve a required client Name from the vat information"))
        case _ =>
          registration.otherAddress.flatMap(_.tradingName)
          .getOrElse(throw new IllegalStateException("Unable to retrieve a required client Name from the display registration information"))
      }

    clientCompanyName + " "
  }
}

object RegistrationWrapper {
  implicit val format: OFormat[RegistrationWrapper] = Json.format[RegistrationWrapper]
}
