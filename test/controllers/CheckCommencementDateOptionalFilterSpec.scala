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
import controllers.routes
import models.Period
import models.requests.OptionalDataRequest
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckCommencementDateOptionalFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(startReturnPeriod: Period) extends CheckCommencementDateOptionalFilterImpl(startReturnPeriod) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when there are periods to be submitted" in {

      val application = applicationBuilder(None).build()

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }


    "must redirect when there are no periods to be submitted" in {
      val application = applicationBuilder(None).build()

      val registration = registrationWrapper.copy(registration =
        registrationWrapper.registration.copy(schemeDetails =
          registrationWrapper.registration.schemeDetails.copy(commencementDate = period.lastDay.plusDays(1))))

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, vrn, iossNumber, registration, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result.value mustEqual Redirect(routes.NoOtherPeriodsAvailableController.onPageLoad())
      }
    }
  }

}
