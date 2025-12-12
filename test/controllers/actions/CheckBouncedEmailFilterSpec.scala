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
import models.requests.RegistrationRequest
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckBouncedEmailFilterSpec extends SpecBase with MockitoSugar {

  class Harness extends CheckBouncedEmailFilterImpl() {
    def callFilter(request: RegistrationRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "when the unusable email status is true" - {

      "must redirect to Intercept Unusable Email" in {

        val app = applicationBuilder(None)
          .build()

        val regWrapperWithUnusableEmail = registrationWrapper.copy(registration =
          registrationWrapper.registration.copy(schemeDetails =
            registrationWrapper.registration.schemeDetails.copy(unusableStatus = true)))

        running(app) {
          val request = RegistrationRequest(FakeRequest(), testCredentials, Some(vrn), "Company Name", iossNumber, regWrapperWithUnusableEmail, None, enrolments)
          val controller = new Harness

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(controllers.routes.InterceptUnusableEmailController.onPageLoad())

        }
      }

    }

    "when the unusable email status is false" - {

      "must be None" in {

        val app = applicationBuilder(None)
          .build()

        running(app) {
          val request = RegistrationRequest(FakeRequest(), testCredentials, Some(vrn), "Company Name", iossNumber, registrationWrapper, None, enrolments)
          val controller = new Harness

          val result = controller.callFilter(request).futureValue

          result mustBe None

        }
      }

    }

  }
}
