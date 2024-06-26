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

package controllers.actions

import base.SpecBase
import models.RegistrationWrapper
import models.requests.{OptionalDataRequest, RegistrationRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import repositories.SessionRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(repository: SessionRepository) extends DataRetrievalAction(repository) {

    def callTransform(request: RegistrationRequest[_]): Future[OptionalDataRequest[_]] =
      transform(request)
  }

  "Data Retrieval Action" - {

    val registrationWrapper = arbitrary[RegistrationWrapper].sample.value

    "when there is no data in the cache" - {

      "must set userAnswers to `None` in the request" in {

        val repository = mock[SessionRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, enrolments)

        when(repository.get(any())) thenReturn Future.successful(None)

        val action = new Harness(repository)

        val result = action.callTransform(request).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must add the userAnswers to the request" in {

        val repository = mock[SessionRepository]
        val request    = RegistrationRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, enrolments)

        when(repository.get(any())) thenReturn Future.successful(Some(emptyUserAnswers))

        val action = new Harness(repository)

        val result = action.callTransform(request).futureValue

        result.userAnswers.value mustEqual emptyUserAnswers
      }
    }
  }
}
