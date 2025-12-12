/*
 * Copyright 2025 HM Revenue & Customs
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


import config.FrontendAppConfig
import connectors.IdentityVerificationConnector
import controllers.auth.{routes => authRoutes}
import models.iv._
import models.iv.IdentityVerificationResult._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps
import views.html.iv.IdentityProblemView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class IdentityVerificationController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                val controllerComponents: MessagesControllerComponents,
                                                ivConnector: IdentityVerificationConnector,
                                                view: IdentityProblemView,
                                                frontendAppConfig: FrontendAppConfig
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController with I18nSupport {

  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(frontendAppConfig.allowedRedirectUrls: _*)

  def identityError(continueUrl: RedirectUrl): Action[AnyContent] = Action { implicit request =>
    Ok(view(continueUrl.get(redirectPolicy).url))
  }

  private val allPossibleEvidences: List[IdentityVerificationEvidenceSource] =
    List(PayslipService, P60Service, NtcService, Passport, CallValidate)

  private def allSourcesDisabled(disabledSources: List[IdentityVerificationEvidenceSource]): Boolean =
    allPossibleEvidences.forall(disabledSources.contains)

  def handleIvFailure(continueUrl: RedirectUrl, journeyId: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>

      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      journeyId
        .fold(Redirect(authRoutes.IdentityVerificationController.identityError(continueUrl).url).toFuture) { id =>
          ivConnector.getJourneyStatus(id).flatMap { _.fold(Redirect(authRoutes.IvReturnController.error(continueUrl).url).toFuture) {
            case InsufficientEvidence => handleInsufficientEvidence(continueUrl)
            case Success => Redirect(continueUrl.get(redirectPolicy).url).toFuture
            case Incomplete => Redirect(authRoutes.IvReturnController.incomplete(continueUrl).url).toFuture
            case FailedMatching => Redirect(authRoutes.IvReturnController.failedMatching(continueUrl).url).toFuture
            case FailedIdentityVerification => Redirect(authRoutes.IvReturnController.failed(continueUrl).url).toFuture
            case UserAborted => Redirect(authRoutes.IvReturnController.userAborted(continueUrl).url).toFuture
            case LockedOut => Redirect(authRoutes.IvReturnController.lockedOut(continueUrl).url).toFuture
            case PrecondFailed => Redirect(authRoutes.IvReturnController.preconditionFailed(continueUrl).url).toFuture
            case TechnicalIssue => Redirect(authRoutes.IvReturnController.technicalIssue(continueUrl).url).toFuture
            case TimeOut => Redirect(authRoutes.IvReturnController.timeout(continueUrl).url).toFuture
            case _ => Redirect(authRoutes.IvReturnController.technicalIssue(continueUrl).url).toFuture
          }}
        }
  }

  private def handleInsufficientEvidence(continueUrl: RedirectUrl)(implicit hc: HeaderCarrier): Future[Result] =
    ivConnector.getDisabledEvidenceSource().map {
      case list if allSourcesDisabled(list) => Redirect(authRoutes.IvReturnController.notEnoughEvidenceSources(continueUrl).url)
      case _ => Redirect(authRoutes.IvReturnController.insufficientEvidence(continueUrl).url)
    }
}
