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

package controllers

import controllers.actions._
import forms.StartReturnFormProvider
import models.etmp.EtmpExclusion
import models.{Period, UserAnswers}
import pages.{StartReturnPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PartialReturnPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.StartReturnView

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: StartReturnFormProvider,
                                       partialReturnPeriodService: PartialReturnPeriodService,
                                       view: StartReturnView,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = (
    cc.authAndGetOptionalData
      andThen cc.checkExcludedTraderOptional(period)
      andThen cc.checkCommencementDateOptional(period)
      andThen cc.checkIsCurrentReturnPeriodFilter(period)).async {

    implicit request =>
      // TODO check for starting correct period

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      val nextPeriod = period.getNext.firstDay

      val isFinalReturn = maybeExclusion.fold(false) { exclusions =>
        nextPeriod.isAfter(exclusions.effectiveDate)
      }

      partialReturnPeriodService.getPartialReturnPeriod(request.registrationWrapper, period).map { maybePartialReturnPeriod =>

        Ok(view(form, waypoints, period, maybeExclusion, isFinalReturn, maybePartialReturnPeriod))
      }

  }

  def onSubmit(waypoints: Waypoints, period: Period): Action[AnyContent] = (
    cc.authAndGetOptionalData
      andThen cc.checkExcludedTraderOptional(period)
      andThen cc.checkCommencementDateOptional(period)
      andThen cc.checkIsCurrentReturnPeriodFilter(period)).async {

    implicit request =>

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      val nextPeriod = period.getNext.firstDay

      val isFinalReturn = maybeExclusion.fold(false) { exclusions =>
        nextPeriod.isAfter(exclusions.effectiveDate)
      }

      form.bindFromRequest().fold(
        formWithErrors =>

          partialReturnPeriodService.getPartialReturnPeriod(request.registrationWrapper, period).map { maybePartialReturnPeriod =>
            BadRequest(view(formWithErrors, waypoints, period, maybeExclusion, isFinalReturn, maybePartialReturnPeriod))

          },

        value => {

          val defaultUserAnswers = UserAnswers(id = request.userId, period = period, lastUpdated = Instant.now(clock))

          val (clearSession: Boolean, userAnswers: UserAnswers) = request.userAnswers match {
            case Some(userAnswers) if userAnswers.period == period => (false, userAnswers)
            case Some(userAnswers) => (true, defaultUserAnswers)
            case _ => (true, defaultUserAnswers)
          }

          for {
            answers <- userAnswers.toFuture
            _ <- if (clearSession) {
              cc.sessionRepository.clear(answers.id)
            } else {
              Future.successful()
            }
            updatedAnswers <- Future.fromTry(answers.set(StartReturnPage(period), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(StartReturnPage(period).navigate(waypoints, answers, updatedAnswers).route)
        }
      )
  }
}
