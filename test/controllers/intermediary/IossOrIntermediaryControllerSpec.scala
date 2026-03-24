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

package controllers.intermediary

import base.SpecBase
import config.FrontendAppConfig
import controllers.actions.FakeGetRegistrationActionProvider
import forms.IossOrIntermediaryFormProvider
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import views.html.IossOrIntermediaryView

class IossOrIntermediaryControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new IossOrIntermediaryFormProvider()
  private val form = formProvider()

  private val bothIossAndIntermediaryEnrolments: Enrolments = Enrolments(
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

  "IossOrIntermediaryController" - {

    ".onPageLoad" - {

      "must return OK and the correct view for a GET" - {

        "when both ioss and intermediary enrolments are present" in {

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

            val request = FakeRequest(GET, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IossOrIntermediaryView]

            val expectedForm = formProvider()
            val expectedEnrolmentIdentifiers = Seq(iossNumber, intermediaryNumber)

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(expectedForm, iossNumber, expectedEnrolmentIdentifiers, totalNumberOfEnrolments = 2)
                (request, messages(application)).toString
          }
        }

        "when only ioss enrolment is present" in {

          val onlyIossEnrolment: Enrolments = Enrolments(
            Set(
              Enrolment(
                key = iossEnrolmentKey,
                identifiers = Seq(
                  EnrolmentIdentifier("IOSSNumber", iossNumber)
                ),
                state = "Activated"
              )
            )
          )

          val fakeProvider =
            new FakeGetRegistrationActionProvider(
              registrationWrapper,
              maybeIntermediaryNumber = Some(intermediaryNumber),
              enrolments = Some(onlyIossEnrolment)
            )

          val application = applicationBuilder(
            getRegistrationAction = Some(fakeProvider)
          ).build()

          running(application) {

            val request = FakeRequest(GET, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IossOrIntermediaryView]

            val expectedForm = formProvider()
            val expectedEnrolmentIdentifiers = Seq(iossNumber)

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(expectedForm, iossNumber, expectedEnrolmentIdentifiers, totalNumberOfEnrolments = 1)
                (request, messages(application)).toString
          }
        }

        "when only intermediary enrolment is present" in {

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

            val request = FakeRequest(GET, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)

            val result = route(application, request).value

            val view = application.injector.instanceOf[IossOrIntermediaryView]

            val expectedForm = formProvider()
            val expectedEnrolmentIdentifiers = Seq(intermediaryNumber)

            status(result) `mustBe` OK
            contentAsString(result) `mustBe`
              view(expectedForm, iossNumber, expectedEnrolmentIdentifiers, totalNumberOfEnrolments = 1)
                (request, messages(application)).toString
          }
        }
      }
    }

    ".onSubmit" - {

      "must redirect to YourAccount page if an ioss option is selected" in {

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

          val request = FakeRequest(POST, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)
            .withFormUrlEncodedBody(("value", iossNumber))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` controllers.routes.YourAccountController.onPageLoad(waypoints).url
        }
      }

      "must redirect to intermediary dashboard if an intermediary option is selected" in {

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
          val config = application.injector.instanceOf[FrontendAppConfig]

          val request = FakeRequest(POST, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)
            .withFormUrlEncodedBody(("value", intermediaryNumber))

          val result = route(application, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` config.intermediaryDashboardUrl
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

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

          val request = FakeRequest(POST, controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad(iossNumber).url)
            .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[IossOrIntermediaryView]

          val result = route(application, request).value

          status(result) `mustBe` BAD_REQUEST
          contentAsString(result) `mustBe`
            view(boundForm, iossNumber, Seq(iossNumber, intermediaryNumber), totalNumberOfEnrolments = 2)
              (request, messages(application)).toString()
        }
      }
    }
  }
}
