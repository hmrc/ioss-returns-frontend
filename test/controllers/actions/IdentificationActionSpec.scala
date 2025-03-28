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
import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.TestAuthRetrievals._
import controllers.routes
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.mvc.{Action, AnyContent, DefaultActionBuilder, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AccountService, UrlBuilderService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private type RetrievalsType = Option[Credentials] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel
  private val vatEnrolmentWithNoIossEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatEnrolmentWithIoss = Enrolments(Set(
    Enrolment(
      "HMRC-MTD-VAT",
      Seq(EnrolmentIdentifier("VRN", "123456789")),
      "Activated"
    ),
    Enrolment(
      "HMRC-IOSS-ORG",
      Seq(EnrolmentIdentifier("IOSSNumber", "IM9001234567")),
      "Activated"
    )
  ))

  class Harness(authAction: IdentifierAction, defaultAction: DefaultActionBuilder) {
    def onPageLoad(): Action[AnyContent] = (defaultAction andThen authAction) { _ => Results.Ok }
  }

  private val mockAuthConnector: AuthConnector = mock[AuthConnector]
  private val mockAccountService: AccountService = mock[AccountService]

  val urlBuilder: Application => UrlBuilderService =
    (application: Application) => application.injector.instanceOf[UrlBuilderService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockAccountService)
  }

  "Identifier Action" - {

    "when the user is logged in as an Organisation Admin with a VAT enrolment and strong credentials" - {

      "when user has ioss enrolment must succeed" in {

        val application = applicationBuilder(None).build()

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithIoss ~ Some(Organisation) ~ ConfidenceLevel.L50))
        when(mockAccountService.getLatestAccount()(any())) thenReturn iossNumber.toFuture

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe OK
        }
      }


      "and no ioss enrolment must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).build()

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoIossEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user is logged in as an Individual with a VAT enrolment, strong credentials and confidence level 250" - {

      "when user has ioss enrolment must succeed" in {

        val application = applicationBuilder(None).build()

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithIoss ~ Some(Individual) ~ ConfidenceLevel.L250))
        when(mockAccountService.getLatestAccount()(any())) thenReturn iossNumber.toFuture

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe OK
        }
      }

      "and no active registration must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).build()

        when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(testCredentials) ~ vatEnrolmentWithNoIossEnrolment ~ Some(Individual) ~ ConfidenceLevel.L250))

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Organisation Admin with strong credentials but no vat enrolment" - {

      "must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Organisation) ~ ConfidenceLevel.L50))

          val action = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less then 250" - {

      "must be redirected to uplift their confidence level" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any())).thenReturn(
            Future.successful(Some(testCredentials) ~ vatEnrolmentWithIoss ~ Some(Individual) ~ ConfidenceLevel.L50)
          )

          val action = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest(GET,"/fake"))

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must startWith(s"${appConfig.ivUpliftUrl}?origin=IOSS&confidenceLevel=250")
        }
      }
    }

    "when the user has logged in as an Individual without a VAT enrolment" - {

      "must be redirected to the Not Registered page" in {

        val application = applicationBuilder(None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some(testCredentials) ~ Enrolments(Set.empty) ~ Some(Individual) ~ ConfidenceLevel.L200))

          val action = new IdentifierAction(mockAuthConnector, mockAccountService, appConfig, urlBuilder(application))
          val controller = new Harness(action, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(None).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new MissingBearerToken),
            mockAccountService,
            appConfig,
            urlBuilder(application)
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new BearerTokenExpired),
            mockAccountService,
            appConfig,
            urlBuilder(application)
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest(GET, "/example"))

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value must startWith(appConfig.loginUrl)
        }
      }
    }

    "the user used an unsupported auth provider" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAuthProvider),
            mockAccountService,
            appConfig,
            urlBuilder(application)
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new UnsupportedAffinityGroup),
            mockAccountService,
            appConfig,
            urlBuilder(application)
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }

    "the user has weak credentials" - {

      "must redirect the user to the Not Registered page" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val appConfig = application.injector.instanceOf[FrontendAppConfig]
          val actionBuilder = application.injector.instanceOf[DefaultActionBuilder]

          val authAction = new IdentifierAction(new FakeFailingAuthConnector(new IncorrectCredentialStrength),
            mockAccountService,
            appConfig,
            urlBuilder(application)
          )
          val controller = new Harness(authAction, actionBuilder)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe SEE_OTHER

          redirectLocation(result).value mustEqual routes.NotRegisteredController.onPageLoad().url
        }
      }
    }
  }
}

class FakeFailingAuthConnector @Inject()(exceptionToReturn: Throwable) extends AuthConnector {
  val serviceUrl: String = ""

  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
