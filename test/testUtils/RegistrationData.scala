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

package testUtils

import base.SpecBase
import models.etmp._
import models.{Bic, Country, Iban}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

object RegistrationData extends SpecBase {

  val eisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  val etmpEuPreviousRegistrationDetails: EtmpPreviousEuRegistrationDetails = EtmpPreviousEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    registrationNumber = arbitrary[String].sample.value,
    schemeType = arbitrary[SchemeType].sample.value,
    intermediaryNumber = Some(arbitrary[String].sample.value)
  )

  val etmpDisplayEuRegistrationDetails: EtmpDisplayEuRegistrationDetails = EtmpDisplayEuRegistrationDetails(
    issuedBy = arbitrary[Country].sample.value.code,
    vatNumber = Some(Gen.alphaNumStr.sample.value),
    taxIdentificationNumber = None,
    fixedEstablishmentTradingName = arbitraryEtmpTradingName.arbitrary.sample.value.tradingName,
    fixedEstablishmentAddressLine1 = arbitrary[String].sample.value,
    fixedEstablishmentAddressLine2 = Some(arbitrary[String].sample.value),
    townOrCity = arbitrary[String].sample.value,
    regionOrState = Some(arbitrary[String].sample.value),
    postcode = Some(arbitrary[String].sample.value)
  )
  val etmpSchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = LocalDate.now,
    euRegistrationDetails = Seq(etmpDisplayEuRegistrationDetails),
    previousEURegistrationDetails = Seq(etmpEuPreviousRegistrationDetails),
    websites = Seq(arbitrary[EtmpWebsite].sample.value),
    contactName = arbitrary[String].sample.value,
    businessTelephoneNumber = arbitrary[String].sample.value,
    businessEmailId = arbitrary[String].sample.value,
    unusableStatus = false,
    nonCompliantReturns = None,
    nonCompliantPayments = None,
  )

  val etmpDisplaySchemeDetails: EtmpSchemeDetails = EtmpSchemeDetails(
    commencementDate = etmpSchemeDetails.commencementDate,
    euRegistrationDetails = Seq(etmpDisplayEuRegistrationDetails),
    previousEURegistrationDetails = etmpSchemeDetails.previousEURegistrationDetails,
    websites = etmpSchemeDetails.websites,
    contactName = etmpSchemeDetails.contactName,
    businessTelephoneNumber = etmpSchemeDetails.businessTelephoneNumber,
    businessEmailId = etmpSchemeDetails.businessEmailId,
    unusableStatus = false,
    nonCompliantReturns = etmpSchemeDetails.nonCompliantReturns,
    nonCompliantPayments = etmpSchemeDetails.nonCompliantPayments
  )

  val genBankDetails: EtmpBankDetails = EtmpBankDetails(
    accountName = arbitrary[String].sample.value,
    bic = Some(arbitrary[Bic].sample.value),
    iban = arbitrary[Iban].sample.value
  )


  val etmpAdminUse: EtmpAdminUse = EtmpAdminUse(
    changeDate = Some(LocalDateTime.now())
  )

  val maxTradingNames: Int = 10

  val etmpDisplayRegistration: EtmpDisplayRegistration = EtmpDisplayRegistration(
    customerIdentification = arbitraryEtmpCustomerIdentification.arbitrary.sample.value,
    tradingNames = Gen.listOfN(maxTradingNames, arbitraryEtmpTradingName.arbitrary).sample.value,
    schemeDetails = etmpDisplaySchemeDetails,
    bankDetails = Some(genBankDetails),
    otherAddress = None,
    exclusions = Gen.listOfN(3, arbitrary[EtmpExclusion]).sample.value,
    adminUse = etmpAdminUse
  )


}

