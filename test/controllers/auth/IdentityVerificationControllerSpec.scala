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

package controllers.auth

import base.SpecBase
import connectors.IdentityVerificationConnector
import models.UserAnswers
import models.iv.IdentityVerificationResult._
import models.iv._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.FutureSyntax.FutureOps
import views.html.iv.IdentityProblemView

class IdentityVerificationControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val continueUrl = "http://localhost/foo"
  private val journeyId = Some("bar")

  private val mockIdentityVerificationConnector: IdentityVerificationConnector = mock[IdentityVerificationConnector]

  private def buildApplication: Application =
    applicationBuilder(None).overrides(bind[IdentityVerificationConnector].toInstance(mockIdentityVerificationConnector)).build()

  override def beforeEach(): Unit = {
    reset(mockIdentityVerificationConnector)
    super.beforeEach()
  }

  def emptyUserAnswersWithVatInfo: UserAnswers = emptyUserAnswers
  registrationWrapper

  "IdentityVerification Controller" - {

    ".identityError" - {

      "must return OK and correct view" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswersWithVatInfo)).build()

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.identityError(RedirectUrl("http://localhost/")).url)

          val view = application.injector.instanceOf[IdentityProblemView]
          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) mustBe view(continueUrl)(request, messages(application)).toString
        }
      }
    }

    ".handleIvFailure" - {

      "must redirect to the continue URL when the result was Success" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(Success).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl(continueUrl), journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe continueUrl
        }
      }

      "must redirect to the Incomplete page when the result was Incomplete" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(Incomplete).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.incomplete(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Failed Matching page when the result was FailedMatching" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(FailedMatching).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.failedMatching(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Failed page when the result was Failed Identity Verification" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(FailedIdentityVerification).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.failed(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Not Enough Evidence Sources page when the result was Insufficient Evidence and all evidence sources are off" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(InsufficientEvidence).toFuture

        when(mockIdentityVerificationConnector.getDisabledEvidenceSource()(any())) thenReturn
          List(PayslipService, P60Service, NtcService, Passport, CallValidate).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.notEnoughEvidenceSources(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Insufficient Evidence page when the result was Insufficient Evidence and some evidence sources are on" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(InsufficientEvidence).toFuture

        when(mockIdentityVerificationConnector.getDisabledEvidenceSource()(any())) thenReturn List(PayslipService).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.insufficientEvidence(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Locked Out page when the result was Locked Out" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(LockedOut).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.lockedOut(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the User Aborted page when the result was User Aborted" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(UserAborted).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.userAborted(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Timeout page when the result was Timeout" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(TimeOut).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.timeout(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Technical Issue page when the result was Technical Issue" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(TechnicalIssue).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.technicalIssue(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the Precondition Failed page when the result was Precondition Failed" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn Some(PrecondFailed).toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.preconditionFailed(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the IvReturnController error page when the result was None" in {

        when(mockIdentityVerificationConnector.getJourneyStatus(any())(any())) thenReturn None.toFuture

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"),journeyId).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IvReturnController.error(RedirectUrl("http://localhost/")).url
        }
      }

      "must redirect to the IdentityVerificationController identityError page when the journey id is missing" in {

        val application = buildApplication

        running(application) {
          val request = FakeRequest(GET, routes.IdentityVerificationController.handleIvFailure(RedirectUrl("http://localhost/"), None).url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.IdentityVerificationController.identityError(RedirectUrl("http://localhost/")).url
        }
      }
    }
  }
}
