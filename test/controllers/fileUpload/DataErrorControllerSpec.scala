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

package controllers.fileUpload

import base.SpecBase
import forms.DataErrorFormProvider
import models.upscan.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.fileUpload.{CsvValidationErrorsPage, DataErrorPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.fileUpload.DataErrorView

class DataErrorControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new DataErrorFormProvider()
  val form: Form[Boolean] = formProvider()
  private val csvErrors: Seq[CsvError] = Seq(CsvError.BlankCell(row = 4, column = CsvColumn.C))
  private val userAnswersWithErrors = emptyUserAnswers.set(CsvValidationErrorsPage(iossNumber), csvErrors).success.value

  lazy val dataErrorRoute: String = controllers.fileUpload.routes.DataErrorController.onPageLoad(waypoints, iossNumber).url

  "DataError Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(Some(userAnswersWithErrors)).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DataErrorView]
        val msgs = messages(application)

        val expectedParagraphs: Seq[String] =
          Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          form,
          waypoints,
          iossNumber,
          period,
          false,
          companyName,
          csvErrors,
          expectedParagraphs,
          false,
          Nil
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithErrors.set(DataErrorPage(iossNumber), true).success.value

      val application = applicationBuilder(Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val view = application.injector.instanceOf[DataErrorView]

        val result = route(application, request).value

        val msgs = messages(application)

        val expectedParagraphs: Seq[String] =
          Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          form.fill(true),
          waypoints,
          iossNumber,
          period,
          false,
          companyName,
          csvErrors,
          expectedParagraphs,
          false,
          Nil
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(DataErrorPage(iossNumber), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` DataErrorPage(iossNumber).navigate(waypoints, emptyUserAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithErrors)).build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DataErrorView]

        val result = route(application, request).value

        val msgs = messages(application)

        val expectedParagraphs: Seq[String] =
          Seq(msgs("dataError.errorMessage.blankCell.p1", "C4"))

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(
          boundForm,
          waypoints,
          iossNumber,
          period,
          false,
          companyName,
          csvErrors,
          expectedParagraphs,
          false,
          Nil
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, dataErrorRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, dataErrorRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
