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
import models.etmp.EtmpVatReturn
import models.{Country, Period}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}

class CorrectionService @Inject()(
                                   vatReturnConnector: VatReturnConnector
                                 )(implicit ec: ExecutionContext) extends Logging {

  def getAccumulativeVatForCountryTotalAmount(
                                               periodFrom: Period,
                                               periodTo: Period,
                                               country: Country
                                             )(implicit hc: HeaderCarrier): Future[(Boolean, BigDecimal)] = {
    for {
      etmpVatReturnList <- getAllReturnsInPeriodRange(periodFrom, periodTo)
    } yield {
      val firstReturn = etmpVatReturnList.head
      val firstReturnVatAmountsDeclaredOnCountry = firstReturn.goodsSupplied.filter(_.msOfConsumption == country.code).map(_.vatAmountGBP)

      val otherReturnsCorrectionsAmountsForCorrectionPeriodAndCountry = etmpVatReturnList.tail.flatMap { etmpVatReturn =>
        etmpVatReturn.correctionPreviousVATReturn.filter(correctionPreviousVATReturn =>
            correctionPreviousVATReturn.msOfConsumption == country.code && correctionPreviousVATReturn.periodKey == periodFrom.toEtmpPeriodString
          ).map(_.totalVATAmountCorrectionGBP)
      }

      val isPreviouslyDeclaredCountry: Boolean = firstReturnVatAmountsDeclaredOnCountry.nonEmpty ||
        otherReturnsCorrectionsAmountsForCorrectionPeriodAndCountry.nonEmpty

      (isPreviouslyDeclaredCountry, firstReturnVatAmountsDeclaredOnCountry.sum + otherReturnsCorrectionsAmountsForCorrectionPeriodAndCountry.sum)
    }
  }

  private def getAllReturnsInPeriodRange(
                                          correctionReturnPeriod: Period,
                                          returnPeriod: Period
                                        )(implicit hc: HeaderCarrier): Future[Seq[EtmpVatReturn]] = {
    Future.sequence(
      getAllPeriods(correctionReturnPeriod, returnPeriod)
        .sortBy(_.firstDay)
        .map(period =>
          vatReturnConnector.get(period)
            .map {
              case Left(error) =>
                val message = s"Error when trying to retrieve vat return from getAllPeriods with error: ${error.body}"
                logger.error(message)
                throw new Exception(message)
              case Right(etmpVatReturn) => etmpVatReturn
            }
        )
    )
  }

  private def getAllPeriods(periodFrom: Period, periodTo: Period): Seq[Period] = {

    @tailrec
    def getAllPeriodsInRange(currentPeriods: Seq[Period], periodFrom: Period, periodTo: Period): Seq[Period] = {
      (periodFrom, periodTo) match {
        case (pf, pt) if pf < pt =>
          val updatedPeriod = currentPeriods :+ pf
          getAllPeriodsInRange(updatedPeriod, pf.getNext, pt)
        case _ => currentPeriods
      }
    }

    getAllPeriodsInRange(Seq.empty, periodFrom, periodTo)
  }
}
