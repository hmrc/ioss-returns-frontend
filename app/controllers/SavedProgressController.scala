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

package controllers

import config.FrontendAppConfig
import connectors.{SaveForLaterConnector, VatReturnConnector}
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.Period
import models.requests.SaveForLaterRequest
import models.responses.ConflictFound
import models.saveForLater.SavedUserAnswers
import pages.SavedProgressPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.SavedProgressView

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavedProgressController @Inject()(
                                         cc: AuthenticatedControllerComponents,
                                         view: SavedProgressView,
                                         connector: SaveForLaterConnector,
                                         vatReturnConnector: VatReturnConnector,
                                         appConfig: FrontendAppConfig,
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(period: Period, continueUrl: RedirectUrl): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val answersExpiry = request.userAnswers.lastUpdated.plus(appConfig.saveForLaterTtl, ChronoUnit.DAYS)
        .atZone(ZoneId.systemDefault()).toLocalDate.format(dateTimeFormatter)
      val safeContinueUrl = continueUrl.get(OnlyRelative).url

      Future.fromTry(request.userAnswers.set(SavedProgressPage, safeContinueUrl)).flatMap { updatedAnswers =>
          val s4LRequest = SaveForLaterRequest(updatedAnswers, request.iossNumber, period)
          (for{
            maybeSavedExternalUrl <- vatReturnConnector.getSavedExternalEntry()
            s4laterResult <- if (request.isIntermediary) {
              connector.submitForIntermediary(s4LRequest)
            } else {
              connector.submit(s4LRequest)
            }
          } yield {
            val externalUrl = maybeSavedExternalUrl.fold(_ => None, _.url)
            (s4laterResult, externalUrl)
          }).flatMap {
            case (Right(Some(_: SavedUserAnswers)), externalUrl) =>
              for {
                _ <- cc.sessionRepository.set(updatedAnswers)
              } yield {
                val determinedRedirect = if (request.isIntermediary) {
                  Some(appConfig.intermediaryDashboardUrl)
                } else {
                  externalUrl
                }
                Ok(view(period, answersExpiry, safeContinueUrl, determinedRedirect))
              }

            case (Left(ConflictFound), externalUrl) if request.isIntermediary =>
              Redirect(appConfig.intermediaryDashboardUrl).toFuture

            case (Left(ConflictFound), externalUrl) =>
              Redirect(externalUrl.getOrElse(routes.YourAccountController.onPageLoad().url)).toFuture

            case (Left(e), _) =>
              logger.error(s"Unexpected result on submit: ${e.toString}")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture

            case (Right(None), _) =>
              logger.error(s"Unexpected result on submit")
              Redirect(routes.JourneyRecoveryController.onPageLoad()).toFuture
          }
      }
  }
}
