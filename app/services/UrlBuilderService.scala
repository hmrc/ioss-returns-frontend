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

package services

import config.FrontendAppConfig
import com.google.inject.Inject
import play.api.mvc.Request

import controllers.auth.{routes => authRoutes}

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.http.HeaderCarrierConverter

class UrlBuilderService @Inject()(config: FrontendAppConfig) {

  def loginContinueUrl(request: Request[_]): RedirectUrl = {

    val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val returnUrl = RedirectUrl(
      request
        .getQueryString("k")
        .orElse(hc.sessionId.map(_.value))
        .map(sessionId => config.loginContinueUrl + request.path + "?k=" + sessionId)
        .getOrElse {
          request.uri
        }
    )

    returnUrl
  }

  def ivFailureUrl(request: Request[_]): String =
    config.loginContinueUrl + authRoutes.IdentityVerificationController.handleIvFailure(loginContinueUrl(request), None).url
}
