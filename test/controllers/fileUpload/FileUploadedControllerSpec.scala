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
import connectors.FileUploadOutcomeConnector
import forms.FileUploadedFormProvider
import models.upscan.FileUploadOutcome
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.JourneyRecoveryPage
import pages.fileUpload.{FileReferencePage, FileUploadedPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps
import views.html.fileUpload.FileUploadedView

class FileUploadedControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new FileUploadedFormProvider()
  val successForm: Form[Boolean] = formProvider.successForm
  val failedForm: Form[Boolean] = formProvider.failedForm
  private val successfulOutcome = FileUploadOutcome(
    fileName = Some("test.csv"),
    status = "READY",
    failureReason = None
  )
  private val failedOutcome = FileUploadOutcome(
    fileName = Some("test.csv"),
    status = "FAILED",
    failureReason = Some("REJECTED")
  )
  private val userAnswersWithRef = emptyUserAnswers.set(FileReferencePage(iossNumber), "fake-ref").success.value
  private val mockOutcomeConnector = mock[FileUploadOutcomeConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val fileUploadedRoute: String = controllers.fileUpload.routes.FileUploadedController.onPageLoad(waypoints, iossNumber).url

  "FileUploaded Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(successfulOutcome).toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRef))
        .overrides(bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileUploadedRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[FileUploadedView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(successForm, waypoints, iossNumber, period, false, companyName, Some(successfulOutcome))(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(successfulOutcome).toFuture

      val userAnswers = userAnswersWithRef.set(FileUploadedPage(iossNumber), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileUploadedRoute)

        val view = application.injector.instanceOf[FileUploadedView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(successForm.fill(true), waypoints, iossNumber, period, false, companyName, Some(successfulOutcome))(request, messages(application)).toString
      }
    }

    "must redirect to the CheckAnswersPage if no corrections available when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(successfulOutcome).toFuture
      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithRef))
          .overrides(
            bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, fileUploadedRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(FileUploadedPage(iossNumber), true).success.value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` FileUploadedPage(iossNumber).navigate(waypoints, emptyUserAnswers, expectedAnswers).route.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(successfulOutcome).toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRef))
        .overrides(bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, fileUploadedRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = successForm.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[FileUploadedView]

        val result = route(application, request).value

        status(result) `mustBe` BAD_REQUEST
        contentAsString(result) `mustBe` view(boundForm, waypoints, iossNumber, period, false, companyName, Some(successfulOutcome))(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, fileUploadedRoute)

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, fileUploadedRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must display failed status when non-CSV file was uploaded" in {

      val nonCsvOutcome = FileUploadOutcome(
        fileName = Some("not-a-csv.pdf"),
        status = "FAILED",
        failureReason = Some("InvalidArgument")
      )

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(nonCsvOutcome).toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRef))
        .overrides(bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileUploadedRoute)
        val view = application.injector.instanceOf[FileUploadedView]

        val result = route(application, request).value

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(
          failedForm,
          waypoints,
          iossNumber,
          period,
          false,
          companyName,
          Some(nonCsvOutcome)
        )(request, messages(application)).toString
      }
    }

    "must display failed status and correct error message when upload failed" in {

      when(mockOutcomeConnector.getOutcome(eqTo("fake-ref"))(any())) thenReturn Some(failedOutcome).toFuture

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRef))
        .overrides(bind[FileUploadOutcomeConnector].toInstance(mockOutcomeConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, fileUploadedRoute)

        val result = route(application, request).value
        val body = contentAsString(result)

        status(result) `mustBe` OK
        
        body must include(messages(application)("fileUploaded.status.failed"))
        
        body must include(messages(application)("fileUploaded.status.error.rejected"))
      }
    }
  }
}
