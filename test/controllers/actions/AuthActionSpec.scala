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

package controllers.actions

import com.google.inject.Inject
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class AuthActionSpec extends BaseSpec with BeforeAndAfterEach {

  private type RetrievalsType = Option[String] ~ Enrolments ~ Option[AffinityGroup] ~ ConfidenceLevel ~ Option[CredentialRole]
  private val vatEnrolment = Enrolments(Set(Enrolment("HMRC-MTD-VAT", Seq(EnrolmentIdentifier("VRN", "123456789")), "Activated")))
  private val vatEnrolment2 = Enrolments(Set(Enrolment("HMCE-VATDEC-ORG", Seq(EnrolmentIdentifier("VATRegNo", "123456789")), "Activated")))

  class Harness(authAction: AuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuthConnector)
  }

  "Auth Action" - {

    "when the user is logged in as an Organisation Admin with a VAT enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(User)))

          val action = new AuthActionImpl(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user is logged in as an Organisation Admin with a VATDEC enrolment and strong credentials" - {

      "must succeed" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment2 ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(User)))

          val action = new AuthActionImpl(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user is logged in as an Individual with a VAT enrolment, strong credentials and confidence level 200" - {

      "must succeed" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L200 ~ None))

          val action = new AuthActionImpl(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual OK
        }
      }
    }

    "when the user has logged in as an Organisation Assistant with a VAT enrolment and strong credentials" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment ~ Some(Organisation) ~ ConfidenceLevel.L50 ~ Some(Assistant)))

          val action = new AuthActionImpl(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual UNAUTHORIZED
        }
      }
    }

    "when the user has logged in as an Individual with a VAT enrolment and strong credentials, but confidence level less than 200" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          when(mockAuthConnector.authorise[RetrievalsType](any(), any())(any(), any()))
            .thenReturn(Future.successful(Some("id") ~ vatEnrolment ~ Some(Individual) ~ ConfidenceLevel.L50 ~ None))

          val action = new AuthActionImpl(mockAuthConnector, bodyParsers)
          val controller = new Harness(action)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustEqual UNAUTHORIZED
        }
      }
    }

    "when the user hasn't logged in" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new MissingBearerToken), bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "the user's session has expired" - {

      "must return Unauthorized " in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new BearerTokenExpired), bodyParsers)
          val controller = new Harness(authAction)
          val request    = FakeRequest(GET, "/foo")
          val result = controller.onPageLoad()(request)

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "the user used an unsupported auth provider" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new UnsupportedAuthProvider), bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "the user has an unsupported affinity group" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new UnsupportedAffinityGroup), bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }
    }

    "the user has weak credentials" - {

      "must return Unauthorized" in {

        val application = applicationBuilder.build()

        running(application) {
          val bodyParsers = application.injector.instanceOf[BodyParsers.Default]

          val authAction = new AuthActionImpl(new FakeFailingAuthConnector(new IncorrectCredentialStrength), bodyParsers)
          val controller = new Harness(authAction)
          val result = controller.onPageLoad()(FakeRequest())

          status(result) mustBe UNAUTHORIZED
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
