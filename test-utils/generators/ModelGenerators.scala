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

package generators

import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import models._
import models.etmp._
import models.financialdata.Charge
import models.payments.{Payment, PaymentStatus}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import queries.{OptionalSalesAtVatRate, SalesToCountryWithOptionalSales, VatRateWithOptionalSalesFromCountry}

import java.time.{LocalDate, LocalDateTime, Month}
import scala.math.BigDecimal.RoundingMode

trait ModelGenerators {

  self: Generators =>

  implicit val arbitraryBigDecimal: Arbitrary[BigDecimal] =
    Arbitrary {
      for {
        nonDecimalNumber <- arbitrary[Int]
        decimalNumber <- arbitrary[Int].retryUntil(_ > 0).retryUntil(_.toString.reverse.head.toString != "0")
      } yield BigDecimal(s"$nonDecimalNumber.$decimalNumber")
    }

  implicit lazy val arbitraryCountry: Arbitrary[Country] =
    Arbitrary {
      Gen.oneOf(Country.euCountries)
    }

  implicit val arbitraryNetSales: BigDecimal = arbitrary[BigDecimal].sample.head

  implicit val arbitraryVatOnSales: Arbitrary[VatOnSales] =
    Arbitrary {
      for {
        choice <- Gen.oneOf(VatOnSalesChoice.values)
        amount <- arbitrary[BigDecimal]
      } yield VatOnSales(choice, amount)
    }

  implicit lazy val salesAtVatRate: Arbitrary[OptionalSalesAtVatRate] =
    Arbitrary {
      for {
        netValueOfSales <- arbitrary[BigDecimal]
        vatOnSales <- arbitraryVatOnSales.arbitrary
      } yield OptionalSalesAtVatRate(Some(netValueOfSales), Some(vatOnSales))
    }

  implicit def arbitraryVatRateFromCountry: Arbitrary[VatRateFromCountry] =
    Arbitrary {
      for {
        rate <- Gen.choose[BigDecimal](BigDecimal(1), BigDecimal(100))
        rateType <- Gen.oneOf(VatRateType.values)
        validFrom <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2100, 1, 1))
      } yield VatRateFromCountry(rate.setScale(2, RoundingMode.HALF_EVEN), rateType, validFrom, Some(validFrom.plusYears(1)))
    }

  implicit val arbitraryOptionalSalesAtVatRate: Arbitrary[OptionalSalesAtVatRate] =
    Arbitrary {
      for {
        netValueOfSales <- Gen.option(Gen.choose[BigDecimal](BigDecimal(minCurrencyAmount.bigDecimal), BigDecimal(maxCurrencyAmount.bigDecimal)))
        vatOnSales <- Gen.option(arbitraryVatOnSales.arbitrary)
      } yield OptionalSalesAtVatRate(netValueOfSales, vatOnSales)
    }

  implicit val arbitraryVatRateWithOptionalSalesFromCountry: Arbitrary[VatRateWithOptionalSalesFromCountry] =
    Arbitrary {
      for {
        rate <- Gen.choose[BigDecimal](BigDecimal(1), BigDecimal(100))
        rateType <- Gen.oneOf(VatRateType.values)
        validFrom <- datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2100, 1, 1))
        validUntil <- Gen.option(datesBetween(LocalDate.of(2021, 7, 1), LocalDate.of(2100, 1, 1)))
        salesAtVatRate <- Gen.option(arbitraryOptionalSalesAtVatRate.arbitrary)
      } yield VatRateWithOptionalSalesFromCountry(rate, rateType, validFrom, validUntil, salesAtVatRate)
    }

  implicit val arbitrarySalesToCountryWithOptionalSales: Arbitrary[SalesToCountryWithOptionalSales] =
    Arbitrary {
      for {
        country <- arbitraryCountry.arbitrary
        vatRatesFromCountry <- Gen.option(Gen.listOfN(3, arbitraryVatRateWithOptionalSalesFromCountry.arbitrary))
      } yield SalesToCountryWithOptionalSales(country, vatRatesFromCountry)
    }

  implicit val arbitraryPeriod: Arbitrary[Period] =
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099)
        quarter <- Gen.oneOf(Month.values)
      } yield Period(year, quarter)
    }

   val arbitraryPeriodKey: Arbitrary[String] = {
    Arbitrary {
      for {
        year <- Gen.choose(2022, 2099).map(_.toString)
        monthKey <- Gen.oneOf("AA", "AB", "AC", "AD", "AE", "AF", "AG", "AH", "AI", "AJ", "AK", "AL")
      } yield s"${year.substring(2, 4)}$monthKey"
    }
  }

  implicit lazy val arbitraryBic: Arbitrary[Bic] = {
    val asciiCodeForA = 65
    val asciiCodeForN = 78
    val asciiCodeForP = 80
    val asciiCodeForZ = 90

    Arbitrary {
      for {
        firstChars <- Gen.listOfN(6, Gen.alphaUpperChar).map(_.mkString)
        char7 <- Gen.oneOf(Gen.alphaUpperChar, Gen.choose(2, 9))
        char8 <- Gen.oneOf(
          Gen.choose(asciiCodeForA, asciiCodeForN).map(_.toChar),
          Gen.choose(asciiCodeForP, asciiCodeForZ).map(_.toChar),
          Gen.choose(0, 9)
        )
        lastChars <- Gen.option(Gen.listOfN(3, Gen.oneOf(Gen.alphaUpperChar, Gen.numChar)).map(_.mkString))
      } yield Bic(s"$firstChars$char7$char8${lastChars.getOrElse("")}").get
    }
  }

  implicit lazy val arbitraryIban: Arbitrary[Iban] =
    Arbitrary {
      Gen.oneOf(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      ).map(v => Iban(v).toOption.get)
    }

  implicit lazy val arbitraryDesAddress: Arbitrary[DesAddress] =
    Arbitrary {
      for {
        line1 <- arbitrary[String]
        line2 <- Gen.option(arbitrary[String])
        line3 <- Gen.option(arbitrary[String])
        line4 <- Gen.option(arbitrary[String])
        line5 <- Gen.option(arbitrary[String])
        postCode <- Gen.option(arbitrary[String])
        countryCode <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString)
      } yield DesAddress(line1, line2, line3, line4, line5, postCode, countryCode)
    }

  implicit val arbitraryVatInfo: Arbitrary[VatCustomerInfo] = Arbitrary {
    for {
      address <- arbitrary[DesAddress]
      registrationDate <- arbitrary[LocalDate]
      partOfVatGroup <- arbitrary[Boolean]
      organisationName <- Gen.option(arbitrary[String])
      individualName <- arbitrary[String]
      singleMarketIndicator <- arbitrary[Boolean]
      deregistrationDecisionDate <- Gen.option(arbitrary[LocalDate])
      overseasIndicator <- arbitrary[Boolean]

    } yield VatCustomerInfo(
      address,
      Some(registrationDate),
      partOfVatGroup,
      organisationName,
      if (organisationName.isEmpty) {
        Some(individualName)
      } else {
        None
      },
      singleMarketIndicator,
      deregistrationDecisionDate,
      overseasIndicator
    )
  }

  implicit lazy val arbitraryEtmpTradingName: Arbitrary[EtmpTradingName] =
    Arbitrary {
      for {
        tradingName <- Gen.alphaStr
      } yield EtmpTradingName(tradingName)
    }

  implicit lazy val arbitraryVatNumberTraderId: Arbitrary[VatNumberTraderId] =
    Arbitrary {
      for {
        vatNumber <- Gen.alphaNumStr
      } yield VatNumberTraderId(vatNumber)
    }

  implicit val arbitraryEtmpEuRegistrationDetails: Arbitrary[EtmpDisplayEuRegistrationDetails] = {
    Arbitrary {
      for {
        issuedBy <- Gen.listOfN(2, Gen.alphaChar).map(_.mkString.toUpperCase)
        vatNumber <- arbitrary[String]
        tradingName <- arbitrary[String]
        fixedEstablishmentAddressLine1 <- arbitrary[String]
        fixedEstablishmentAddressLine2 <- Gen.option(arbitrary[String])
        townOrCity <- arbitrary[String]
        regionOrState <- Gen.option(arbitrary[String])
        postcode <- Gen.option(arbitrary[String])
      } yield {
        EtmpDisplayEuRegistrationDetails(
          issuedBy,
          Some(vatNumber),
          None,
          tradingName,
          fixedEstablishmentAddressLine1,
          fixedEstablishmentAddressLine2,
          townOrCity,
          regionOrState,
          postcode
        )
      }
    }
  }

  implicit val arbitraryEtmpPreviousEURegistrationDetails: Arbitrary[EtmpPreviousEuRegistrationDetails] = {
    Arbitrary {
      for {
        issuedBy <- arbitrary[String]
        registrationNumber <- arbitrary[String]
        schemeType <- Gen.oneOf(SchemeType.values)
        intermediaryNumber <- Gen.option(arbitrary[String])
      } yield EtmpPreviousEuRegistrationDetails(issuedBy, registrationNumber, schemeType, intermediaryNumber)
    }
  }

  implicit lazy val arbitraryWebsite: Arbitrary[EtmpWebsite] =
    Arbitrary {
      for {
        websiteAddress <- Gen.alphaStr
      } yield EtmpWebsite(websiteAddress)
    }

  implicit val arbitraryEtmpSchemeDetails: Arbitrary[EtmpSchemeDetails] = {
    Arbitrary {
      for {
        commencementDate <- arbitrary[String]
        euRegistrationDetails <- Gen.listOfN(5, arbitraryEtmpEuRegistrationDetails.arbitrary)
        previousEURegistrationDetails <- Gen.listOfN(5, arbitraryEtmpPreviousEURegistrationDetails.arbitrary)
        websites <- Gen.listOfN(10, arbitraryWebsite.arbitrary)
        contactName <- arbitrary[String]
        businessTelephoneNumber <- arbitrary[String]
        businessEmailId <- arbitrary[String]
        nonCompliantReturns <- Gen.option(arbitrary[Int].toString)
        nonCompliantPayments <- Gen.option(arbitrary[Int].toString)
      } yield
        EtmpSchemeDetails(
          commencementDate,
          euRegistrationDetails,
          previousEURegistrationDetails,
          websites,
          contactName,
          businessTelephoneNumber,
          businessEmailId,
          unusableStatus = false,
          nonCompliantReturns,
          nonCompliantPayments
        )
    }
  }

  implicit lazy val arbitraryEtmpBankDetails: Arbitrary[EtmpBankDetails] =
    Arbitrary {
      for {
        accountName <- arbitrary[String]
        bic <- Gen.option(arbitrary[Bic])
        iban <- arbitrary[Iban]
      } yield EtmpBankDetails(accountName, bic, iban)
    }

  implicit lazy val arbitraryEtmpExclusion: Arbitrary[EtmpExclusion] = {
    Arbitrary {
      for {
        exclusionReason <- Gen.oneOf[EtmpExclusionReason](EtmpExclusionReason.values)
        effectiveDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        decisionDate <- arbitrary[Int].map(n => LocalDate.ofEpochDay(n))
        quarantine <- arbitrary[Boolean]
      } yield EtmpExclusion(
        exclusionReason,
        effectiveDate,
        decisionDate,
        quarantine
      )
    }
  }

  implicit lazy val arbitraryAdminUse: Arbitrary[EtmpAdminUse] =
    Arbitrary {
      for {
        changeDate <- arbitrary[LocalDateTime]
      } yield EtmpAdminUse(Some(changeDate))
    }

  implicit val arbitraryEtmpDisplayRegistration: Arbitrary[EtmpDisplayRegistration] = Arbitrary {
    for {
      etmpTradingNames <- Gen.listOfN(2, arbitraryEtmpTradingName.arbitrary)
      schemeDetails <- arbitrary[EtmpSchemeDetails]
      bankDetails <- arbitrary[EtmpBankDetails]
      exclusions <- Gen.listOfN(1, arbitraryEtmpExclusion.arbitrary)
      adminUse <- arbitrary[EtmpAdminUse]
    } yield EtmpDisplayRegistration(
      etmpTradingNames,
      schemeDetails,
      bankDetails,
      exclusions,
      adminUse
    )
  }

  implicit val arbitraryRegistrationWrapper: Arbitrary[RegistrationWrapper] = Arbitrary {
    for {
      vatInfo <- arbitrary[VatCustomerInfo]
      registration <- arbitrary[EtmpDisplayRegistration]
    } yield RegistrationWrapper(vatInfo, registration)
  }

  implicit val arbitraryObligationDetails: Arbitrary[EtmpObligationDetails] =
    Arbitrary {
      for {
        status <- Gen.oneOf(EtmpObligationsFulfilmentStatus.values)
        periodKey <- arbitraryPeriodKey.arbitrary
      } yield EtmpObligationDetails(
        status = status,
        periodKey = periodKey
      )
    }

  implicit val arbitraryObligations: Arbitrary[EtmpObligations] =
    Arbitrary {
      for {
        obligationDetails <- Gen.listOfN(3, arbitrary[EtmpObligationDetails])
      } yield {
        EtmpObligations(Seq(EtmpObligation(
          obligationDetails = obligationDetails
        )))
      }
    }

  implicit val arbitraryEtmpVatReturnGoodsSupply: Arbitrary[EtmpVatReturnGoodsSupplied] =
    Arbitrary {
      for {
        msOfConsumption <- arbitrary[String]
        vatRateType <- Gen.oneOf(EtmpVatRateType.values)
        taxableAmountGBP <- arbitrary[BigDecimal]
        vatAmountGBP <- arbitrary[BigDecimal]
      } yield EtmpVatReturnGoodsSupplied(
        msOfConsumption = msOfConsumption,
        vatRateType = vatRateType,
        taxableAmountGBP = taxableAmountGBP,
        vatAmountGBP = vatAmountGBP
      )
    }

  implicit val arbitraryEtmpVatReturnCorrection: Arbitrary[EtmpVatReturnCorrection] =
    Arbitrary {
      for {
        periodKey <- arbitrary[String]
        periodFrom <- arbitrary[String]
        periodTo <- arbitrary[String]
        msOfConsumption <- arbitrary[String]
        totalVATAmountCorrectionGBP <- arbitrary[BigDecimal]
        totalVATAmountCorrectionEUR <- arbitrary[BigDecimal]
      } yield EtmpVatReturnCorrection(
        periodKey = periodKey,
        periodFrom = periodFrom,
        periodTo = periodTo,
        msOfConsumption = msOfConsumption,
        totalVATAmountCorrectionGBP = totalVATAmountCorrectionGBP,
        totalVATAmountCorrectionEUR = totalVATAmountCorrectionEUR
      )
    }

  implicit val arbitraryEtmpVatReturnBalanceOfVatDue: Arbitrary[EtmpVatReturnBalanceOfVatDue] =
    Arbitrary {
      for {
        msOfConsumption <- arbitrary[String]
        totalVATDueGBP <- arbitrary[BigDecimal]
        totalVATEUR <- arbitrary[BigDecimal]
      } yield EtmpVatReturnBalanceOfVatDue(
        msOfConsumption = msOfConsumption,
        totalVATDueGBP = totalVATDueGBP,
        totalVATEUR = totalVATEUR
      )
    }

  implicit val arbitraryCharge: Arbitrary[Charge] =
    Arbitrary {
      for {
        period <- arbitrary[Period]
        originalAmount <- arbitrary[BigDecimal]
        outstandingAmount <- arbitrary[BigDecimal]
        clearedAmount <- arbitrary[BigDecimal]
      } yield Charge(
        period = period,
        originalAmount = originalAmount,
        outstandingAmount = outstandingAmount,
        clearedAmount = clearedAmount
      )
    }

  implicit val arbitraryEtmpVatReturn: Arbitrary[EtmpVatReturn] =
    Arbitrary {
      for {
        returnReference <- arbitrary[String]
        returnVersion <- arbitrary[LocalDateTime]
        periodKey <- arbitrary[String]
        returnPeriodFrom <- arbitrary[LocalDate]
        returnPeriodTo <- arbitrary[LocalDate]
        amountOfGoodsSupplied <- Gen.oneOf(List(1, 2, 3))
        goodsSupplied <- Gen.listOfN(amountOfGoodsSupplied, arbitrary[EtmpVatReturnGoodsSupplied])
        totalVATGoodsSuppliedGBP <- arbitrary[BigDecimal]
        totalVATAmountPayable <- arbitrary[BigDecimal]
        totalVATAmountPayableAllSpplied <- arbitrary[BigDecimal]
        amountOfCorrections <- Gen.oneOf(List(1, 2, 3))
        correctionPreviousVATReturn <- Gen.listOfN(amountOfCorrections, arbitrary[EtmpVatReturnCorrection])
        totalVATAmountFromCorrectionGBP <- arbitrary[BigDecimal]
        amountOfBalanceOfVATDueForMS <- Gen.oneOf(List(1, 2, 3))
        balanceOfVATDueForMS <- Gen.listOfN(amountOfBalanceOfVATDueForMS, arbitrary[EtmpVatReturnBalanceOfVatDue])
        totalVATAmountDueForAllMSGBP <- arbitrary[BigDecimal]
        paymentReference <- arbitrary[String]
      } yield EtmpVatReturn(
        returnReference = returnReference,
        returnVersion = returnVersion,
        periodKey = periodKey,
        returnPeriodFrom = returnPeriodFrom,
        returnPeriodTo = returnPeriodTo,
        goodsSupplied = goodsSupplied,
        totalVATGoodsSuppliedGBP = totalVATGoodsSuppliedGBP,
        totalVATAmountPayable = totalVATAmountPayable,
        totalVATAmountPayableAllSpplied = totalVATAmountPayableAllSpplied,
        correctionPreviousVATReturn = correctionPreviousVATReturn,
        totalVATAmountFromCorrectionGBP = totalVATAmountFromCorrectionGBP,
        balanceOfVATDueForMS = balanceOfVATDueForMS,
        totalVATAmountDueForAllMSGBP = totalVATAmountDueForAllMSGBP,
        paymentReference = paymentReference
      )
    }

  implicit val arbitraryPayment: Arbitrary[Payment] = {
    Arbitrary {
      for {
        period <- arbitrary[Period]
        amountOwed <- arbitrary[BigDecimal]
        dateDue <- arbitrary[LocalDate]
        paymentStatus <- Gen.oneOf(PaymentStatus.values)
      } yield Payment(
        period = period,
        amountOwed = amountOwed,
        dateDue = dateDue,
        paymentStatus = paymentStatus
      )
    }
  }
}
