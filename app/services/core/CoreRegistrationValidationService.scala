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

package services.core

import connectors.core.ValidateCoreRegistrationConnector
import logging.Logging
import models.core.{CoreRegistrationRequest, Match, SourceType}
import models.etmp.SchemeType
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CoreRegistrationValidationService @Inject()(
                                                   connector: ValidateCoreRegistrationConnector
                                                 )
                                                 (implicit ec: ExecutionContext) extends Logging {

  def searchIossScheme(searchNumber: String, previousScheme: SchemeType, intermediaryNumber: Option[String], countryCode: String)
                      (implicit hc: HeaderCarrier): Future[Option[Match]] = {


    val coreRegistrationRequest = CoreRegistrationRequest(
      SourceType.TraderId.toString,
      Some(convertScheme(previousScheme)),
      searchNumber,
      intermediaryNumber,
      countryCode
    )

    getValidateCoreRegistrationResponse(coreRegistrationRequest)
  }

  private def getValidateCoreRegistrationResponse(coreRegistrationRequest: CoreRegistrationRequest)
                                                 (implicit hc: HeaderCarrier): Future[Option[Match]] = {
    connector.validateCoreRegistration(coreRegistrationRequest).map {
      case Right(coreRegistrationResponse) =>
        coreRegistrationResponse.matches.headOption
      case Left(errorResponse) =>
        logger.error(s"failed getting registration response $errorResponse")
        throw CoreRegistrationValidationException("Error while validating core registration")
    }
  }

  private def convertScheme(previousScheme: SchemeType): String = {
    previousScheme match {
      case SchemeType.IOSSWithoutIntermediary => "IOSS"
      case SchemeType.IOSSWithIntermediary => "IOSS"
      case _ => throw new IllegalStateException("Called with an OSS number, when only IOSS should apply")
    }
  }
}


case class CoreRegistrationValidationException(message: String) extends Exception(message)
