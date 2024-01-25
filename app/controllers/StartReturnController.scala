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

package controllers

import controllers.actions._
import date.LocalDateOps
import forms.StartReturnFormProvider
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, TransferringMSID, VoluntarilyLeaves}
import models.{Period, UserAnswers}
import pages.{StartReturnPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.StartReturnView

import java.time.{Clock, Instant, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: StartReturnFormProvider,
                                       view: StartReturnView,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndGetOptionalData() {
    implicit request =>
      // TODO check for starting correct period

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      maybeExclusion match {
        case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
          LocalDate.now(clock) >= exclusion.effectiveDate =>
          Redirect(routes.CannotUseServiceErrorController.onPageLoad().url)
        case _ => Ok(view(form, waypoints, period))
      }
  }

  def onSubmit(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, waypoints, period)).toFuture,

        value => {

          val initialisedAnswers = UserAnswers(id = request.userId, period = period, lastUpdated = Instant.now(clock))

          for {
            answers <- request.userAnswers.getOrElse(initialisedAnswers).toFuture
            updatedAnswers <- Future.fromTry(answers.set(StartReturnPage(period), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(StartReturnPage(period).navigate(waypoints, answers, updatedAnswers).route)
        }
      )
  }
}
