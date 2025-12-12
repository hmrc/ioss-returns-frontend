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

import connectors.ReturnStatusConnector
import controllers.actions._
import forms.StartReturnFormProvider
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.Reversal
import models.SubmissionStatus.Overdue
import models.{Period, UserAnswers}
import pages.{StartReturnPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PartialReturnPeriodService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.yourAccount.{CurrentReturns, Return}
import views.html.StartReturnView

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StartReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       formProvider: StartReturnFormProvider,
                                       partialReturnPeriodService: PartialReturnPeriodService,
                                       returnStatusConnector: ReturnStatusConnector,
                                       view: StartReturnView,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints, period: Period): Action[AnyContent] = (
    cc.authAndGetOptionalData()
      andThen cc.checkExcludedTraderOptional(period)
      andThen cc.checkCommencementDateOptional(period)
      andThen cc.checkIsCurrentReturnPeriodFilter(period)).async {

    implicit request =>
      // TODO check for starting correct period
      val isIntermediary = request.isIntermediary
      val form: Form[Boolean] = formProvider(isIntermediary)
      val companyName = request.registrationWrapper.getCompanyName()

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      val nextPeriod = period.getNext.firstDay

      val isFinalReturn = maybeExclusion.fold(false) { exclusion =>
        exclusion.exclusionReason != Reversal && nextPeriod.isAfter(exclusion.effectiveDate)
      }

      for {
        maybePartialReturnPeriod <- partialReturnPeriodService.getPartialReturnPeriod(request.iossNumber, request.registrationWrapper, period)
        currentReturnsResponse <- if (isIntermediary) {
          returnStatusConnector.getCurrentReturns(request.iossNumber)
        } else {
          Future.successful(Right(CurrentReturns(returns = Seq.empty, finalReturnsCompleted = false)))
        }
      } yield {
        val overdueReturns: Seq[Return] = currentReturnsResponse match {
          case Right(currentReturns) =>
            currentReturns.returns
              .filter(r => r.submissionStatus == Overdue)
              .sortBy(r => (r.period.year, r.period.month.getValue))
          case Left(_) => Seq.empty
        }
        Ok(view(form, waypoints, period, maybeExclusion, isFinalReturn, maybePartialReturnPeriod, isIntermediary, companyName, overdueReturns))
      }

  }

  def onSubmit(waypoints: Waypoints, period: Period): Action[AnyContent] = (
    cc.authAndGetOptionalData()
      andThen cc.checkExcludedTraderOptional(period)
      andThen cc.checkCommencementDateOptional(period)
      andThen cc.checkIsCurrentReturnPeriodFilter(period)).async {

    implicit request =>
      val isIntermediary = request.isIntermediary
      val form: Form[Boolean] = formProvider(isIntermediary)
      val companyName = request.registrationWrapper.getCompanyName()

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      val nextPeriod = period.getNext.firstDay

      val isFinalReturn = maybeExclusion.fold(false) { exclusion =>
        exclusion.exclusionReason != Reversal && nextPeriod.isAfter(exclusion.effectiveDate)
      }

      form.bindFromRequest().fold(
        formWithErrors =>
          for {
            maybePartialReturnPeriod <- partialReturnPeriodService.getPartialReturnPeriod(request.iossNumber, request.registrationWrapper, period)
            currentReturnsResponse <- if (isIntermediary) {
              returnStatusConnector.getCurrentReturns(request.iossNumber)
            } else {
              Future.successful(Right(CurrentReturns(returns = Seq.empty, finalReturnsCompleted = false)))
            }
          } yield {
            val overdueReturns: Seq[Return] = currentReturnsResponse match {
              case Right(currentReturns) =>
                currentReturns.returns
                  .filter(r => r.submissionStatus == Overdue)
                  .sortBy(r => (r.period.year, r.period.month.getValue))
              case Left(_) => Seq.empty
            }
            BadRequest(view(formWithErrors, waypoints, period, maybeExclusion, isFinalReturn, maybePartialReturnPeriod, isIntermediary, companyName, overdueReturns))
          },

        value => {

          val defaultUserAnswers = UserAnswers(
            id = request.userId,
            iossNumber = request.iossNumber,
            period = period,
            lastUpdated = Instant.now(clock)
          )

          val (clearSession: Boolean, userAnswers: UserAnswers) = request.userAnswers match {
            case Some(userAnswers) if userAnswers.period == period => (false, userAnswers)
            case _ => (true, defaultUserAnswers)
          }

          for {
            answers <- userAnswers.toFuture
            _ <- if (clearSession) {
              cc.sessionRepository.clear(answers.id)
            } else {
              Future.successful(())
            }
            updatedAnswers <- Future.fromTry(answers.set(StartReturnPage(period), value))
            _ <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(StartReturnPage(period).navigate(waypoints, answers, updatedAnswers).route)
        }
      )
  }
}