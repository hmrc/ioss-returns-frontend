/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.actions

import connectors.SaveForLaterConnector
import models.UserAnswers
import models.requests.{OptionalDataRequest, RegistrationRequest}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedAnswersRetrievalAction (repository: SessionRepository, saveForLaterConnector: SaveForLaterConnector)
                          (implicit val executionContext: ExecutionContext)
  extends ActionTransformer[RegistrationRequest, OptionalDataRequest] {

  override protected def transform[A](request: RegistrationRequest[A]): Future[OptionalDataRequest[A]] = {
    val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
    val userAnswers = for {
      answersInSession <- repository.get(request.userId)
      savedForLater <- saveForLaterConnector.get()(hc)
    } yield {
      val answers = if (answersInSession.isEmpty) {
        savedForLater match {
          case Right(Some(answers)) =>
            val newAnswers = UserAnswers(request.userId, request.iossNumber, answers.period, answers.data, answers.lastUpdated)
            repository.set(newAnswers)
            Some(newAnswers)
          case _ => None
        }
      } else {
        answersInSession
      }
      answers
    }

    userAnswers.map {
      OptionalDataRequest(
        request.request,
        request.credentials,
        request.vrn,
        request.iossNumber,
        request.companyName,
        request.registrationWrapper,
        request.intermediaryNumber,
        _
      )
    }
  }
}

class SavedAnswersRetrievalActionProvider @Inject()(repository: SessionRepository, saveForLaterConnector: SaveForLaterConnector)
                                           (implicit ec: ExecutionContext) {

  def apply(): SavedAnswersRetrievalAction =
    new SavedAnswersRetrievalAction(repository, saveForLaterConnector)
}
