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

import base.SpecBase
import models.etmp.*
import play.api.libs.json.{JsError, JsSuccess, Json}
import testUtils.RegistrationData.etmpDisplayRegistration

class RegistrationWrapperSpec extends SpecBase {

  private val desAddress: DesAddress = arbitraryDesAddress.arbitrary.sample.value

  private val vatInfo: VatCustomerInfo = vatCustomerInfo.copy(desAddress = desAddress)

  private val tradingNames: Seq[EtmpTradingName] = etmpDisplayRegistration.tradingNames
  private val displaySchemeDetails: EtmpSchemeDetails = etmpDisplayRegistration.schemeDetails
  private val bankDetails: EtmpBankDetails = etmpDisplayRegistration.bankDetails.get
  private val exclusions: Seq[EtmpExclusion] = etmpDisplayRegistration.exclusions
  private val adminUse: EtmpAdminUse = etmpDisplayRegistration.adminUse

  "RegistrationWrapper" - {

    "must serialise/deserialise to and from RegistrationWrapper" in {

      val registrationWrapper: RegistrationWrapper = RegistrationWrapper(Some(vatInfo), etmpDisplayRegistration)

      val expectedJson = Json.obj(
        "vatInfo" -> Json.obj(
          "desAddress" -> vatInfo.desAddress,
          "singleMarketIndicator" -> vatInfo.singleMarketIndicator,
          "partOfVatGroup" -> vatInfo.partOfVatGroup,
          "registrationDate" -> vatInfo.registrationDate,
          "organisationName" -> vatInfo.organisationName,
          "deregistrationDecisionDate" -> vatInfo.deregistrationDecisionDate,
          "overseasIndicator" -> vatInfo.overseasIndicator
        ),
        "registration" -> Json.obj(
          "tradingNames" -> tradingNames,
          "adminUse" -> adminUse,
          "schemeDetails" -> displaySchemeDetails,
          "exclusions" -> exclusions,
          "bankDetails" -> bankDetails,
        )
      )

      Json.toJson(registrationWrapper) mustBe expectedJson
      expectedJson.validate[RegistrationWrapper] mustBe JsSuccess(registrationWrapper)
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[RegistrationWrapper] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "vatInfo" -> Json.obj(
          "desAddress" -> vatInfo.desAddress,
          "partOfVatGroup" -> vatInfo.partOfVatGroup,
          "registrationDate" -> vatInfo.registrationDate,
          "organisationName" -> vatInfo.organisationName,
          "singleMarketIndicator" -> vatInfo.singleMarketIndicator,
          "overseasIndicator" -> vatInfo.overseasIndicator
        ),
        "registration" -> Json.obj(
          "tradingNames" -> 12345,
          "schemeDetails" -> displaySchemeDetails,
          "bankDetails" -> bankDetails,
          "exclusions" -> exclusions,
          "adminUse" -> adminUse
        )
      )

      expectedJson.validate[RegistrationWrapper] mustBe a[JsError]
    }
  }
}
