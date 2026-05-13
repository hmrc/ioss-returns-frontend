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

package services.intermediary

import base.SpecBase
import config.FrontendAppConfig
import connectors.VatReturnConnector
import models.external.ExternalEntryUrl
import models.responses.InternalServerError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class DashboardNavigationServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  private val mockVatReturnConnector: VatReturnConnector = mock[VatReturnConnector]

  implicit private val hc: HeaderCarrier = new HeaderCarrier()

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockVatReturnConnector
    )
  }

  ".navigateToAppropriateDashboard" - {

    "must return the IossOrIntermediary page url" - {

      "when an intermediary is present and is enrolled to both IOSS and Intermediary services" in {

        val service = new DashboardNavigationService(mockFrontendAppConfig, mockVatReturnConnector)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = true,
          intermediaryEnrolmentsExist = true,
          iossEnrolmentsExist = true
        ).futureValue

        result `mustBe` controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad().url
      }
    }

    "must return the intermediary dashboard url" - {

      "when an intermediary is not enrolled to an IOSS service" in {

        val service = new DashboardNavigationService(mockFrontendAppConfig, mockVatReturnConnector)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = true,
          intermediaryEnrolmentsExist = true,
          iossEnrolmentsExist = false
        ).futureValue

        result `mustBe` mockFrontendAppConfig.intermediaryDashboardUrl
      }
    }

    "when an intermediary is not present must redirect to" - {

      "your account when no external entry url is present" in {

        val externalEntryUrl: ExternalEntryUrl = ExternalEntryUrl(url = None)

        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(externalEntryUrl).toFuture

        val service = new DashboardNavigationService(mockFrontendAppConfig, mockVatReturnConnector)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = false,
          intermediaryEnrolmentsExist = false,
          iossEnrolmentsExist = true
        ).futureValue

        result `mustBe` controllers.routes.YourAccountController.onPageLoad(waypoints).url
        verify(mockVatReturnConnector, times(1)).getSavedExternalEntry()(any())
      }

      "to the external entry url when one is present" in {

        val externalEntryUrl: ExternalEntryUrl = ExternalEntryUrl(url = Some("/test-external-url"))

        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Right(externalEntryUrl).toFuture

        val service = new DashboardNavigationService(mockFrontendAppConfig, mockVatReturnConnector)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = false,
          intermediaryEnrolmentsExist = false,
          iossEnrolmentsExist = true
        ).futureValue

        result `mustBe` externalEntryUrl.url.value
        verify(mockVatReturnConnector, times(1)).getSavedExternalEntry()(any())
      }

      "to your account when the connector returns an error when retrieving an external entry url" in {

        when(mockVatReturnConnector.getSavedExternalEntry()(any())) thenReturn Left(InternalServerError).toFuture

        val service = new DashboardNavigationService(mockFrontendAppConfig, mockVatReturnConnector)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = false,
          intermediaryEnrolmentsExist = false,
          iossEnrolmentsExist = true
        ).futureValue

        result `mustBe` controllers.routes.YourAccountController.onPageLoad(waypoints).url
        verify(mockVatReturnConnector, times(1)).getSavedExternalEntry()(any())
      }
    }
  }
}
