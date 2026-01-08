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
import connectors.{SaveForLaterConnector, VatReturnConnector}
import models.external.ExternalEntryUrl
import models.responses.{ConflictFound, UnexpectedResponseStatus}
import models.saveForLater.SavedUserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import utils.FutureSyntax.FutureOps
import views.html.SavedProgressView

import java.time.format.DateTimeFormatter
import java.time.{Clock, Instant, LocalDate, ZoneId}

class SavedProgressControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val mockSaveForLaterConnector = mock[SaveForLaterConnector]
  private val mockVatReturnConnector = mock[VatReturnConnector]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val cacheTtlAsLong = 1L
  when(mockAppConfig.cacheTtl) thenReturn cacheTtlAsLong

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockVatReturnConnector,
      mockSaveForLaterConnector
    )
  }

  "SavedProgress Controller" - {

    "must return OK and the correct view for a GET and clear user-answers after return submitted" in {

      val instantDate = Instant.now
      val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
      val date = LocalDate.now(stubClock).plusDays(28)

      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val savedAnswers = SavedUserAnswers(
        iossNumber,
        period,
        JsObject(Seq("test" -> Json.toJson("test"))),
        instantDate
      )

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(Some(savedAnswers)).toFuture
      when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Right(true).toFuture
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[VatReturnConnector].toInstance(mockVatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[SavedProgressView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(period, date.format(dateTimeFormatter), "/test")(request, messages(app)).toString
      }
    }

    "must return OK and the correct view for a GET and clear user-answers after return submitted and add the external backToYourAccount url" in {

      val instantDate = Instant.now
      val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
      val date = LocalDate.now(stubClock).plusDays(28)

      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

      val savedAnswers = SavedUserAnswers(
        iossNumber,
        period,
        JsObject(Seq("test" -> Json.toJson("test"))),
        instantDate
      )

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Right(Some(savedAnswers)).toFuture
      when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Right(true).toFuture
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some("example"))).toFuture

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[VatReturnConnector].toInstance(mockVatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[SavedProgressView]

        status(result) `mustBe` OK
        contentAsString(result) `mustBe` view(period, date.format(dateTimeFormatter), "/test", Some("example"))(request, messages(app)).toString
      }
    }

    "must redirect to Your Account Controller when Save For Later Connector returns ConflictFound" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Left(ConflictFound).toFuture
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[VatReturnConnector].toInstance(mockVatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

        val result = route(app, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.YourAccountController.onPageLoad().url
      }
    }

    "must redirect to the external 'Back to your account' url when Save For Later Connector returns ConflictFound" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Left(ConflictFound).toFuture
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(Some("example"))).toFuture

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[VatReturnConnector].toInstance(mockVatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

        val result = route(app, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` "example"
      }
    }

    "must redirect to Journey Recovery Controller when Save For Later Connector returns Error Response" in {

      when(mockSaveForLaterConnector.submit(any())(any())) thenReturn Left(UnexpectedResponseStatus(1, "error")).toFuture
      when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

      val app = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
          bind[VatReturnConnector].toInstance(mockVatReturnConnector)
        ).build()

      running(app) {

        val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

        val result = route(app, request).value

        status(result) `mustBe` SEE_OTHER
        redirectLocation(result).value `mustBe` routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "for an Intermediary" - {

      "must return OK and the correct view for a GET and clear user-answers after return submitted" in {

        val instantDate: Instant = Instant.now()
        val stubClock: Clock = Clock.fixed(instantDate, ZoneId.systemDefault)
        val date = LocalDate.now(stubClock).plusDays(28)

        val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

        val savedAnswers = SavedUserAnswers(
          iossNumber,
          period,
          JsObject(Seq("test" -> Json.toJson("test"))),
          instantDate
        )

        when(mockSaveForLaterConnector.submitForIntermediary(any())(any())) thenReturn Right(Some(savedAnswers)).toFuture
        when(mockSaveForLaterConnector.delete(any())(any())) thenReturn Right(true).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

        val app = applicationBuilder(
          userAnswers = Some(completeUserAnswers.copy(lastUpdated = instantDate)),
          maybeIntermediaryNumber = Some(intermediaryNumber)
        )
          .overrides(
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        running(app) {

          val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

          val config = app.injector.instanceOf[FrontendAppConfig]

          val result = route(app, request).value

          val view = app.injector.instanceOf[SavedProgressView]

          status(result) `mustBe` OK
          contentAsString(result) `mustBe` view(period, date.format(dateTimeFormatter), "/test", Some(config.intermediaryDashboardUrl))(request, messages(app)).toString
        }
      }

      "must redirect to Intermediary Dashboard Your Account Controller when Save For Later Connector returns ConflictFound" in {

        when(mockSaveForLaterConnector.submitForIntermediary(any())(any())) thenReturn Left(ConflictFound).toFuture
        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(ExternalEntryUrl(None)).toFuture

        val app = applicationBuilder(
          userAnswers = Some(completeUserAnswers),
          maybeIntermediaryNumber = Some(intermediaryNumber)
        )
          .overrides(
            bind[SaveForLaterConnector].toInstance(mockSaveForLaterConnector),
            bind[VatReturnConnector].toInstance(mockVatReturnConnector)
          ).build()

        running(app) {

          val request = FakeRequest(GET, routes.SavedProgressController.onPageLoad(period, RedirectUrl("/test")).url)

          val config = app.injector.instanceOf[FrontendAppConfig]

          val result = route(app, request).value

          status(result) `mustBe` SEE_OTHER
          redirectLocation(result).value `mustBe` config.intermediaryDashboardUrl
        }
      }
    }
  }
}
