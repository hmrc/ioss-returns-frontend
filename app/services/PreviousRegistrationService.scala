/*
 * Copyright 2026 HM Revenue & Customs
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

import connectors.{FinancialDataConnector, RegistrationConnector}
import logging.Logging
import models.StandardPeriod
import models.payments.PrepareData
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps
import viewmodels.previousReturns.PreviousRegistration

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PreviousRegistrationService @Inject()(
                                             registrationConnector: RegistrationConnector,
                                             financialDataConnector: FinancialDataConnector
                                           )(implicit ec: ExecutionContext) extends Logging {

  def getPreviousRegistrations(isIntermediary: Boolean)(implicit hc: HeaderCarrier): Future[List[PreviousRegistration]] = {
    if (isIntermediary) {
      List.empty.toFuture
    } else {
      registrationConnector.getAccounts().map { accounts =>
        val accountDetails: Seq[(YearMonth, String)] = accounts
          .enrolments.map(e => e.activationDate -> e.identifiers.find(_.key == "IOSSNumber").map(_.value))
          .collect {
            case (Some(activationDate), Some(iossNumber)) => YearMonth.from(activationDate) -> iossNumber
          }.sortBy(_._1)
  
        accountDetails.zip(accountDetails.drop(1)).map { case ((activationDate, iossNumber), (nextActivationDate, _)) =>
          PreviousRegistration(
            startPeriod = StandardPeriod(activationDate),
            endPeriod = StandardPeriod(nextActivationDate.minusMonths(1)),
            iossNumber = iossNumber
          )
        }.toList
      }
    }
  }

  def getPreviousRegistrationPrepareFinancialData(isIntermediary: Boolean)(implicit hc: HeaderCarrier): Future[List[PrepareData]] = {
    getPreviousRegistrations(isIntermediary).flatMap { previousRegistrations =>
      Future.sequence(
        previousRegistrations.map { previousRegistration =>
          financialDataConnector.prepareFinancialDataWithIossNumber(previousRegistration.iossNumber).map {
            case Right(previousRegistrationPrepareData) => previousRegistrationPrepareData
            case Left(error) =>
              val message = s"There was an issue retrieving prepared financial data ${error.body}"
              logger.error(message)
              throw new Exception(message)
          }
        }
      )
    }
  }
}
