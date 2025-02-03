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

package controllers.testOnly

import config.FrontendAppConfig
import connectors.testOnly.TestOnlyConnector
import controllers.actions.AuthenticatedControllerComponents
import models.Period
import models.external.*
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, UnsafePermitAll}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendController}
import views.html.testonly.AuthLoginStubView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyController @Inject()(
                                    testConnector: TestOnlyConnector,
                                    config: FrontendAppConfig,
                                    cc: AuthenticatedControllerComponents,
                                    authLoginStubView: AuthLoginStubView
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val externalRequest = ExternalRequest("BTA", "/business-account")

  def yourAccountFromExternal(lang: Option[String] = None): Action[AnyContent] = cc.auth.async {
    implicit request =>
      testConnector.externalEntry(externalRequest, YourAccount.name, None, lang) map {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def startReturnFromExternal(period: Period, lang: Option[String] = None): Action[AnyContent] = cc.auth.async {
    implicit request =>
      testConnector.externalEntry(externalRequest, StartReturn.name, Some(period), lang) map {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def continueReturnFromExternal(period: Period, lang: Option[String] = None): Action[AnyContent] = cc.auth.async {
    implicit request =>
      testConnector.externalEntry(externalRequest, ContinueReturn.name, Some(period), lang) map {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def returnsHistoryFromExternal(lang: Option[String] = None): Action[AnyContent] = cc.auth.async {
    implicit request =>
      testConnector.externalEntry(externalRequest, ReturnsHistory.name, None, lang) map {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }
  }

  def paymentsFromExternal(lang: Option[String] = None): Action[AnyContent] = cc.auth.async {
    implicit request =>
      testConnector.externalEntry(externalRequest, Payment.name, None, lang) map {
        case Right(response) => Redirect(response.redirectUrl)
        case Left(_) => InternalServerError
      }

  }

  def quickAuthStub(): Action[AnyContent] = cc.actionBuilder { implicit request =>

    val defaultIossNumber = "IM9001234567"
    val defaultVrn = "100000002"
    val postAction: Call = Call("POST", config.authLoginStubSignInUrl)
    val startUrl: String = s"${config.host}${controllers.routes.YourAccountController.onPageLoad()}"
    val redirectPolicy = UnsafePermitAll

    val iossNumber = request.getQueryString("ioss").getOrElse(defaultIossNumber)
    val vrn = request.getQueryString("vrn").getOrElse(defaultVrn)

    val continueUrl =
      request
        .getQueryString("continue")
        .map(RedirectUrl.apply)
        .map(_.get(redirectPolicy).url)
        .getOrElse(startUrl)

    Ok(authLoginStubView(iossNumber, vrn, continueUrl, postAction)).withNewSession
  }

}
