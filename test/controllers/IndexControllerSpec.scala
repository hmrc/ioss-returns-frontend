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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.FakeGetRegistrationActionProvider
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

class IndexControllerSpec extends SpecBase {

  "Index Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.YourAccountController.onPageLoad(waypoints).url
      }
    }

    "when an intermediary is present" - {

     "must redirect to IossOrIntermediary page if both ioss and intermediary enrolments are present" in {

       val bothIossAndIntermediaryEnrolments: Enrolments = Enrolments(
         Set(
           Enrolment(
             key = iossEnrolmentKey,
             identifiers = Seq(
               EnrolmentIdentifier("IOSSNumber", iossNumber)
             ),
             state = "Activated"
           ),
           Enrolment(
             key = intermediaryEnrolmentKey,
             identifiers = Seq(
               EnrolmentIdentifier("IntNumber", intermediaryNumber)
             ),
             state = "Activated"
           )
         )
       )

       val fakeProvider =
         new FakeGetRegistrationActionProvider(
           registrationWrapper,
           maybeIntermediaryNumber = Some(intermediaryNumber),
           enrolments = Some(bothIossAndIntermediaryEnrolments)
         )

       val application = applicationBuilder(
         getRegistrationAction = Some(fakeProvider)
       ).build()

       running(application) {

         val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

         val result = route(application, request).value

         status(result) mustEqual SEE_OTHER
         redirectLocation(result).value mustEqual
           controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad().url
       }
     }

      "must redirect to the intermediary dashboard if there are no ioss enrolments" in {

        val onlyIntermediaryEnrolment: Enrolments = Enrolments(
          Set(
            Enrolment(
              key = intermediaryEnrolmentKey,
              identifiers = Seq(
                EnrolmentIdentifier("IntNumber", intermediaryNumber)
              ),
              state = "Activated"
            )
          )
        )

        val fakeProvider =
          new FakeGetRegistrationActionProvider(
            registrationWrapper,
            maybeIntermediaryNumber = Some(intermediaryNumber),
            enrolments = Some(onlyIntermediaryEnrolment)
          )

        val application = applicationBuilder(
          getRegistrationAction = Some(fakeProvider)
        ).build()

        running(application) {
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request = FakeRequest(GET, routes.IndexController.onPageLoad.url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual config.intermediaryDashboardUrl
        }
      }
    }
  }
}
