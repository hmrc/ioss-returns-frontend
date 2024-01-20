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

import play.api.libs.json.{Json, OFormat}

case class EtmpDisplayEuRegistrationDetails(
                                             issuedBy: String,
                                             vatNumber: Option[String],
                                             taxIdentificationNumber: Option[String],
                                             fixedEstablishmentTradingName: String,
                                             fixedEstablishmentAddressLine1: String,
                                             fixedEstablishmentAddressLine2: Option[String] = None,
                                             townOrCity: String,
                                             regionOrState: Option[String] = None,
                                             postcode: Option[String] = None
                                           )

object EtmpDisplayEuRegistrationDetails {

  implicit val format: OFormat[EtmpDisplayEuRegistrationDetails] = Json.format[EtmpDisplayEuRegistrationDetails]
}

