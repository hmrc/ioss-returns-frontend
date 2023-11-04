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

import base.SpecBase
import connectors.RegistrationConnector
import models.requests.{IdentifierRequest, RegistrationRequest}
import models.RegistrationWrapper
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.FakeRequest
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetRegistrationActionSpec extends SpecBase with MockitoSugar with EitherValues {

  class Harness(
                 connector: RegistrationConnector
               ) extends GetRegistrationAction(connector) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] =
      refine(request)
  }

  "Get Registration Action" - {

    "and a registration can be retrieved from the backend" - {

      "must save the registration to the repository and return Right" in {

        val registrationWrapper = Arbitrary.arbitrary[RegistrationWrapper].sample.value

        val request = FakeRequest()
        val connector = mock[RegistrationConnector]
        when(connector.get()(any())) thenReturn registrationWrapper.toFuture

        val action = new Harness(connector)

        val result = action.callRefine(IdentifierRequest(request, testCredentials, vrn, iossNumber)).futureValue

        result.isRight mustEqual true
      }
    }


  }
}
