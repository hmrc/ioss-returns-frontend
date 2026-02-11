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

package services.saveForLater

import connectors.SaveForLaterConnector
import logging.Logging
import models.Period
import models.requests.SaveForLaterRequest
import models.saveForLater.SavedUserAnswers
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SaveForLaterService @Inject()(
                                     saveForLaterConnector: SaveForLaterConnector,
                                   )(implicit ec: ExecutionContext) extends Logging {
  
  private def getAllClientSavedAnswers()(implicit hc: HeaderCarrier): Future[Seq[SavedUserAnswers]] = {

    saveForLaterConnector.getForIntermediary().flatMap {
      case Right(savedUserAnswers) => savedUserAnswers.toFuture

      case Left(error) =>
        val errorMessage: String = s"An error occurred retrieving saved user answers: ${error.body}"
        val exception = new Exception(errorMessage)
        logger.error(errorMessage, error)
        throw exception
    }
  }
  
  def getSavedReturnsForClient(iossNumber: String)(implicit hc: HeaderCarrier): Future[Seq[SavedUserAnswers]] = {
    getAllClientSavedAnswers().flatMap { savedUserAnswers =>
      savedUserAnswers.filter(_.iossNumber == iossNumber).toFuture
    }
  }

  def deleteSavedUserAnswers(iossNumber: String, period: Period)(implicit hc: HeaderCarrier): Future[Boolean] = {
    saveForLaterConnector.delete(iossNumber, period).flatMap {
      case Right(isDeleted) => isDeleted.toFuture

      case Left(error) =>
        val errorMessage: String = s"An error occurred deleting saved user answers for period: $period with error: ${error.body}"
        val exception = new Exception(errorMessage)
        logger.error(errorMessage, error)
        throw exception
    }
  }
}
