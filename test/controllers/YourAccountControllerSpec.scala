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

package controllers

import base.SpecBase
import generators.Generators
import models.RegistrationWrapper
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  "Your Account Controller" - {

    "should display your account view" - {
      "when registration wrapper is present" in {

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapper)
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }

  }
}