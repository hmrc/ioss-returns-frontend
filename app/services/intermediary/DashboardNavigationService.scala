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

import config.FrontendAppConfig
import connectors.VatReturnConnector
import controllers.intermediary.routes
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DashboardNavigationService @Inject()(
                                            frontendAppConfig: FrontendAppConfig,
                                            vatReturnConnector: VatReturnConnector
                                          )(implicit ec: ExecutionContext) {

  def getAppropriateDashboardUrl(
                                  isIntermediary: Boolean,
                                  intermediaryEnrolmentsExist: Boolean,
                                  iossEnrolmentsExist: Boolean
                                )(implicit hc: HeaderCarrier): Future[String] = {

    (isIntermediary, intermediaryEnrolmentsExist, iossEnrolmentsExist) match {
      case (true, true, true) => routes.IossOrIntermediaryController.onPageLoad().url.toFuture
      case (true, true, false) => frontendAppConfig.intermediaryDashboardUrl.toFuture
      case _ =>
        for {
          maybeExternalEntryUrl <- getExternalEntry()
        } yield {
          maybeExternalEntryUrl.getOrElse(controllers.routes.YourAccountController.onPageLoad().url)
        }
    }
  }

  private def getExternalEntry()(implicit hc: HeaderCarrier): Future[Option[String]] = {
    for {
      externalEntryResponse <- vatReturnConnector.getSavedExternalEntry()
    } yield {
      externalEntryResponse.fold(
        _ => None,
        _.url
      )
    }
  }
}
