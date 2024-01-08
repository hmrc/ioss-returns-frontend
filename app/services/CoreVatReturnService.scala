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

import connectors.VatReturnConnector
import logging.Logging
import models.{Country, UserAnswers}
import models.core._
import models.corrections.PeriodWithCorrections
import models.requests.DataRequest
import play.api.http.Status.CREATED
import queries.{AllCorrectionPeriodsQuery, AllSalesWithTotalAndVatQuery, VatRateWithOptionalSalesFromCountry}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.Formatters.generateVatReturnReference

import java.time.{Clock, Instant}
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreVatReturnService @Inject()(
                                      returnConnector: VatReturnConnector,
                                      salesAtVatRateService: SalesAtVatRateService,
                                      clock: Clock)(implicit ec: ExecutionContext) extends Logging {

  def submitCoreVatReturn(userAnswers: UserAnswers)(implicit request: DataRequest[_]): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    returnConnector.submit(buildCoreVatReturn(userAnswers)).map {
      case response if response.status == CREATED =>
        logger.info("Successful core vat return submission")
        true
      case response => logger.error(s"Got error while submitting VAT return ${response.status} with body ${response.body}")
        throw new Exception(s"Error while submitting VAT return ${response.status} with body ${response.body}")
    }
  }

  private def buildCoreVatReturn(userAnswers: UserAnswers)(implicit request: DataRequest[_]): CoreVatReturn = {
    val vatReturnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)

    val instantNow = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS)

    CoreVatReturn(
      vatReturnReferenceNumber = vatReturnReference,
      version = instantNow,
      traderId = CoreTraderId(
        request.iossNumber,
        "XI"
      ),
      period = CorePeriod(
        userAnswers.period.year,
        userAnswers.period.month.getValue match {
          case monthValue if monthValue < 10 => s"0$monthValue"
          case monthValue => monthValue.toString
        }
      ),
      startDate = userAnswers.period.firstDay,
      endDate = userAnswers.period.lastDay,
      submissionDateTime = instantNow,
      totalAmountVatDueGBP = salesAtVatRateService.getTotalVatOwedAfterCorrections(userAnswers),
      msconSupplies = getAllMsconSupplies(userAnswers),
      changeDate = request.registrationWrapper.registration.adminUse.changeDate
    )
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
        .getOrElse(throw new IllegalStateException(s"Unable to find sales by country $country when expected"))
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
        grandTotalMsestGoodsGBP = BigDecimal(0),
        correctionsTotalGBP = corrections.map(_.totalVatAmountCorrectionGBP).sum,
        msidSupplies = msidSupplies,
        msestSupplies = List.empty,
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
        correctionPeriodWithCountry.correctionReturnPeriod.month.getValue match {
          case monthValue if monthValue < 10 => s"0$monthValue"
          case monthValue => monthValue.toString
        }
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
