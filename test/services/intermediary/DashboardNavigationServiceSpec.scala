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
import org.scalatestplus.mockito.MockitoSugar

import scala.language.postfixOps

class DashboardNavigationServiceSpec extends SpecBase with MockitoSugar {

  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  ".navigateToAppropriateDashboard" - {

    "must return the IossOrIntermediary page url" - {

      "when an intermediary is present and is enrolled to both IOSS and Intermediary services" in {

        val service = new DashboardNavigationService(mockFrontendAppConfig)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = true,
          intermediaryEnrolmentsExist = true,
          iossEnrolmentsExist = true
        )

        result mustEqual controllers.intermediary.routes.IossOrIntermediaryController.onPageLoad().url
      }
    }

    "must return the intermediary dashboard url" - {

      "when an intermediary is not enrolled to an IOSS service" in {

        val service = new DashboardNavigationService(mockFrontendAppConfig)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = true,
          intermediaryEnrolmentsExist = true,
          iossEnrolmentsExist = false
        )

        result mustEqual mockFrontendAppConfig.intermediaryDashboardUrl
      }
    }

    "must return the IOSS dashboard url" - {

      "when an intermediary is not present in the request" in {

        val service = new DashboardNavigationService(mockFrontendAppConfig)

        val result = service.getAppropriateDashboardUrl(
          isIntermediary = false,
          intermediaryEnrolmentsExist = false,
          iossEnrolmentsExist = true
        )

        result mustEqual controllers.routes.YourAccountController.onPageLoad().url
      }
    }
  }
}
