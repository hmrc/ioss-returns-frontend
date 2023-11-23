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

package controllers.actions

import models.UserAnswers
import models.requests.{OptionalDataRequest, RegistrationRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class FakeDataRetrievalAction(dataToReturn: Option[UserAnswers])
  extends DataRetrievalAction(
    mock[SessionRepository]
  )(ExecutionContext.Implicits.global) {

  override protected def transform[A](request: RegistrationRequest[A]): Future[OptionalDataRequest[A]] =
    Future(OptionalDataRequest(request.request, request.credentials, request.vrn, request.iossNumber, request.registrationWrapper, dataToReturn))
}

class FakeDataRetrievalActionProvider(dataToReturn: Option[UserAnswers])
  extends DataRetrievalActionProvider(mock[SessionRepository])(ExecutionContext.Implicits.global) {

  override def apply(): DataRetrievalAction =
    new FakeDataRetrievalAction(dataToReturn)
}
