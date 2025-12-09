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

package services

import connectors.{IntermediaryRegistrationConnector, VatReturnConnector}
import logging.Logging
import models.core.*
import models.corrections.PeriodWithCorrections
import models.requests.DataRequest
import models.{Country, UserAnswers}
import play.api.http.Status.CREATED
import queries.{AllCorrectionPeriodsQuery, AllSalesWithTotalAndVatQuery, SalesToCountryWithOptionalSales, VatRateWithOptionalSalesFromCountry}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Formatters.generateVatReturnReference
import utils.FutureSyntax.FutureOps

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreVatReturnService @Inject()(
                                      returnConnector: VatReturnConnector,
                                      salesAtVatRateService: SalesAtVatRateService,
                                      intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                      clock: Clock)(implicit ec: ExecutionContext) extends Logging {

  /**
   *
   * @param userAnswers
   * @param request
   * @return = the remaining amount owed
   */
  def submitCoreVatReturn(userAnswers: UserAnswers)(implicit request: DataRequest[_]): Future[BigDecimal] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val totalAmountVatDueGBP = salesAtVatRateService.getTotalVatOwedAfterCorrections(userAnswers)
    buildCoreVatReturn(userAnswers, totalAmountVatDueGBP).flatMap { coreVatReturn =>
      returnConnector.submit(coreVatReturn, request.iossNumber).map {
        case response if response.status == CREATED =>
          logger.info("Successful core vat return submission")
          totalAmountVatDueGBP
        case response => logger.error(s"Got error while submitting VAT return ${response.status} with body ${response.body}")
          throw new Exception(s"Error while submitting VAT return ${response.status} with body ${response.body}")
      }
    }
  }

  private def buildCoreVatReturn(userAnswers: UserAnswers, totalAmountVatDueGBP: BigDecimal)
                                (implicit request: DataRequest[_], hc: HeaderCarrier): Future[CoreVatReturn] = {
    val vatReturnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)

    val instantNow = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS)

    maybeIntermediary.map { maybeInt =>
      CoreVatReturn(
        vatReturnReferenceNumber = vatReturnReference,
        version = instantNow,
        traderId = CoreTraderId(
          request.iossNumber,
          "XI",
          maybeInt

        ),
        period = CorePeriod(
          userAnswers.period.year,
          userAnswers.period.zeroPaddedMonth
        ),
        startDate = userAnswers.period.firstDay,
        endDate = userAnswers.period.lastDay,
        submissionDateTime = instantNow,
        totalAmountVatDueGBP = totalAmountVatDueGBP,
        msconSupplies = getAllMsconSupplies(userAnswers),
        changeDate = request.registrationWrapper.registration.adminUse.changeDate
      )
    }
  }

  private def maybeIntermediary(implicit hc: HeaderCarrier, request: DataRequest[_]): Future[Option[CoreIntermediary]] = {
    val isIntermediary = request.isIntermediary
    val intermediaryNumber = request.intermediaryNumber

    if (!isIntermediary) {
      None.toFuture
    } else {
      intermediaryNumber match {
        case Some(intNumber) =>
          intermediaryRegistrationConnector.get(intNumber).map { intermediaryReg =>
            Some(CoreIntermediary(
              issuedBy = "XI",
              intNumber = intNumber,
              intChangeDate = intermediaryReg.etmpDisplayRegistration.adminUse.changeDate
            ))
          }
        case None =>
          None.toFuture
      }
    }
  }

  private def getAllMsconSupplies(userAnswers: UserAnswers): List[CoreMsconSupply] = {
    val allSales = userAnswers.get(AllSalesWithTotalAndVatQuery)
    val allCorrections = userAnswers.get(AllCorrectionPeriodsQuery)
    val allSalesCountries = allSales.getOrElse(List.empty).map(_.country)
    val allCorrectionsCountries = allCorrections.getOrElse(List.empty).flatMap(_.correctionsToCountry.getOrElse(List.empty).map(_.correctionCountry))
    val allCountries = (allSalesCountries ++ allCorrectionsCountries).distinct
    val balancesByCountry = salesAtVatRateService.getVatOwedToCountries(userAnswers)

    allCountries.map { country =>

      val allSalesByCountry = allSales
        .getOrElse(List.empty)
        .find(_.country == country)
        .getOrElse(SalesToCountryWithOptionalSales(country, None))
      val allVatRatesByCountry = allSalesByCountry.vatRatesFromCountry.getOrElse(List.empty)
      val msidSupplies = getMsidSuppliesFrom(allVatRatesByCountry)

      val allCorrectionsByCountry = allCorrections
        .getOrElse(List.empty)
        .filter(_.correctionsToCountry.getOrElse(List.empty).exists(_.correctionCountry == country))
      val corrections = getCoreCorrections(allCorrectionsByCountry, country)

      CoreMsconSupply(
        msconCountryCode = country.code,
        balanceOfVatDueGBP = balancesByCountry
          .find(_.country == country)
          .getOrElse(throw new IllegalStateException(s"Unable to find balance for country $country when expected")).totalVat,
        grandTotalMsidGoodsGBP = msidSupplies.map(_.vatAmountGBP).sum,
        correctionsTotalGBP = corrections.map(_.totalVatAmountCorrectionGBP).sum,
        msidSupplies = msidSupplies,
        corrections = corrections
      )
    }
  }

  private def getMsidSuppliesFrom(allVatRatesWithSales: List[VatRateWithOptionalSalesFromCountry]): List[CoreSupply] = {
    allVatRatesWithSales.map { vatRateWithSales =>
      val taxableAmount = vatRateWithSales
        .salesAtVatRate
        .flatMap(_.netValueOfSales)
        .getOrElse(throw new IllegalStateException(s"Unable to get taxable amount for vat rate ${vatRateWithSales.rate}"))
      val vatAmount = vatRateWithSales
        .salesAtVatRate
        .flatMap(_.vatOnSales.map(_.amount))
        .getOrElse(throw new IllegalStateException(s"Unable to get VAT amount for vat rate ${vatRateWithSales.rate}"))

      CoreSupply(
        supplyType = "GOODS",
        vatRate = vatRateWithSales.rate,
        vatRateType = vatRateWithSales.rateType.toString,
        taxableAmountGBP = taxableAmount,
        vatAmountGBP = vatAmount
      )
    }
  }

  private def getCoreCorrections(allCorrectionsWithCountry: List[PeriodWithCorrections], country: Country): List[CoreCorrection] = {
    allCorrectionsWithCountry.map { correctionPeriodWithCountry =>
      val correctionPeriod = CorePeriod(
        correctionPeriodWithCountry.correctionReturnPeriod.year,
        correctionPeriodWithCountry.correctionReturnPeriod.zeroPaddedMonth
      )

      val correctionAmount = correctionPeriodWithCountry
        .correctionsToCountry
        .getOrElse(List.empty)
        .filter(_.correctionCountry == country)
        .map(_.countryVatCorrection.getOrElse(throw new IllegalStateException(s"Unable to get correction amount for country $country with correction period $correctionPeriod")))
        .sum

      CoreCorrection(
        period = correctionPeriod,
        totalVatAmountCorrectionGBP = correctionAmount
      )

    }
  }

}
