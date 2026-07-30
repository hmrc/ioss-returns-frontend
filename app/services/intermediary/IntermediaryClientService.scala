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

package services.intermediary

import connectors.IntermediaryRegistrationConnector
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IntermediaryClientService @Inject()(intermediaryRegistrationConnector: IntermediaryRegistrationConnector)(implicit ec: ExecutionContext) {

  def getClientName(
                     isIntermediary: Boolean,
                     intermediaryNumber: Option[String],
                     iossNumber: String
                   )(implicit hc: HeaderCarrier): Future[Option[String]] = {

    if (isIntermediary) {
      intermediaryNumber match {
        case Some(number) =>
          intermediaryRegistrationConnector
            .get(number)
            .map { registration =>
              registration.etmpDisplayRegistration.clientDetails
                .find(_.clientIossID == iossNumber)
                .map(_.clientName)
            }

        case None =>
          Future.failed(
            new RuntimeException("No intermediary number in request")
          )
      }
    } else {
      Future.successful(None)
    }
  }
}

