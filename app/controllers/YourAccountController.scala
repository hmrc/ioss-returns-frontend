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

import config.FrontendAppConfig
import connectors.CurrentReturnHttpParser.CurrentReturnsResponse
import connectors.PrepareDataHttpParser.PrepareDataResponse
import connectors._
import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.SubmissionStatus.Excluded
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, Reversal, TransferringMSID, VoluntarilyLeaves}
import models.payments._
import models.requests.RegistrationRequest
import models.{Period, SubmissionStatus, UserAnswers}
import pages.Waypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.PreviousRegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PaymentsViewModel
import viewmodels.yourAccount.{CurrentReturns, ReturnsViewModel}
import views.html.YourAccountView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       financialDataConnector: FinancialDataConnector,
                                       saveForLaterConnector: SaveForLaterConnector,
                                       view: YourAccountView,
                                       returnStatusConnector: ReturnStatusConnector,
                                       previousRegistrationService: PreviousRegistrationService,
                                       clock: Clock,
                                       sessionRepository: SessionRepository,
                                       appConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>
      val results: Future[(CurrentReturnsResponse, PrepareDataResponse, Option[UserAnswers])] = getCurrentReturns()

      if (request.enrolments.enrolments.count(_.key == appConfig.iossEnrolment) > 1) {
        previousRegistrationService.getPreviousRegistrationPrepareFinancialData().flatMap { prepareDataList =>
          prepareView(results, prepareDataList, waypoints)
        }
      } else {
        prepareView(results, List.empty, waypoints)
      }
  }

  private def prepareView(
                           results: Future[(CurrentReturnsResponse, PrepareDataResponse, Option[UserAnswers])],
                           previousRegistrationPrepareData: List[PrepareData],
                           waypoints: Waypoints
                         )(implicit request: RegistrationRequest[AnyContent]): Future[Result] = {
    results.map {
      case (Right(availableReturns), Right(vatReturnsWithFinancialData), answers) =>
        preparedViewWithFinancialData(availableReturns, vatReturnsWithFinancialData, previousRegistrationPrepareData, waypoints, answers.map(_.period))
      case (Left(error), error2, _) =>
        logger.error(s"there was an error with period with status $error and getting periods with outstanding amounts $error2")
        throw new Exception(error.toString)
      case (left, right, _) =>
        val message = s"There was an error during period with status $left $right"
        logger.error(message)
        throw new Exception(message)
    }
  }

  private def getCurrentReturns()(implicit request: RegistrationRequest[AnyContent]):
  Future[(CurrentReturnsResponse, PrepareDataResponse, Option[UserAnswers])] = {
    for {
      currentReturns <- returnStatusConnector.getCurrentReturns(request.iossNumber)
      currentPayments <- financialDataConnector.prepareFinancialData()
      userAnswers <- getSavedAnswers()
    } yield {
      userAnswers.map(answers => sessionRepository.set(answers))
      (currentReturns, currentPayments, userAnswers)
    }
  }

  private def getSavedAnswers()(implicit request: RegistrationRequest[AnyContent]): Future[Option[UserAnswers]] = {
    for {
      answersInSession <- sessionRepository.get(request.userId)
      savedForLater <- saveForLaterConnector.get()
    } yield {
      val answers = if (answersInSession.isEmpty) {
        savedForLater match {
          case Right(Some(answers)) => Some(UserAnswers(request.userId, answers.period, answers.data, answers.lastUpdated))
          case _ => None
        }
      } else {
        answersInSession
      }
      answers
    }
  }

  private def preparedViewWithFinancialData(
                                             currentReturns: CurrentReturns,
                                             currentPayments: PrepareData,
                                             previousRegistrationPrepareData: List[PrepareData],
                                             waypoints: Waypoints,
                                             periodInProgress: Option[Period]
                                           )(implicit request: RegistrationRequest[AnyContent]): Result = {

    val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

    val now: LocalDate = LocalDate.now(clock)

    val leaveThisServiceUrl = if (maybeExclusion.isEmpty || maybeExclusion.exists(_.exclusionReason == Reversal)) {
      Some(appConfig.leaveThisServiceUrl)
    } else {
      None
    }

    val rejoinUrl = if (request.registrationWrapper.registration.canRejoinRegistration(now) && !getExistsOutstandingReturns(currentReturns)) {
      Some(appConfig.rejoinThisServiceUrl)
    } else {
      None
    }

    val returnsViewModel = buildReturnsViewModel(currentReturns, periodInProgress)

    val paymentsViewModel = PaymentsViewModel(currentPayments.duePayments, currentPayments.overduePayments,
      currentPayments.excludedPayments, clock)

    val hasDeregistered = hasDeregisteredFromVat(request)

    Ok(view(
      waypoints,
      businessName = request.registrationWrapper.vatInfo.getName,
      iossNumber = request.iossNumber,
      paymentsViewModel = paymentsViewModel,
      changeYourRegistrationUrl = appConfig.amendRegistrationUrl,
      rejoinRegistrationUrl = rejoinUrl,
      leaveThisServiceUrl = leaveThisServiceUrl,
      cancelYourRequestToLeaveUrl = cancelYourRequestToLeaveUrl(maybeExclusion),
      exclusionsEnabled = appConfig.exclusionsEnabled,
      maybeExclusion = maybeExclusion,
      hasSubmittedFinalReturn = currentReturns.finalReturnsCompleted,
      returnsViewModel = returnsViewModel,
      previousRegistrationPrepareData = previousRegistrationPrepareData,
      hasDeregisteredFromVat = hasDeregistered
    ))
  }

  private def buildReturnsViewModel(currentReturns: CurrentReturns, periodInProgress: Option[Period])
                                   (implicit request: RegistrationRequest[AnyContent]) = {
    ReturnsViewModel(
      returns = currentReturns.returns.map(currentReturn => if (periodInProgress.contains(currentReturn.period)) {
        currentReturn.copy(inProgress = true)
      } else {
        currentReturn
      }),
      excludedReturns = currentReturns.completeOrExcludedReturns.filter(_.submissionStatus == Excluded),
      clock
    )
  }

  private def getExistsOutstandingReturns(currentReturns: CurrentReturns): Boolean = {
    val existsOutstandingReturn = {
      if (currentReturns.finalReturnsCompleted) {
        false
      } else {
        currentReturns.returns.exists { currentReturn =>
          Seq(SubmissionStatus.Due, SubmissionStatus.Overdue, SubmissionStatus.Next).contains(currentReturn.submissionStatus) &&
            !isOlderThanThreeYears(currentReturn.dueDate, clock)
        }
      }
    }
    existsOutstandingReturn
  }

  private def cancelYourRequestToLeaveUrl(maybeExclusion: Option[EtmpExclusion]): Option[String] = {
    maybeExclusion match {
      case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
        LocalDate.now(clock).isBefore(exclusion.effectiveDate) => Some(appConfig.cancelYourRequestToLeaveUrl)
      case _ => None
    }
  }

  private def hasDeregisteredFromVat(request: RegistrationRequest[AnyContent]): Boolean = {
      request.registrationWrapper.vatInfo match {
        case vatInfo if vatInfo.deregistrationDecisionDate.isDefined =>
          true
        case _ =>
          false
      }
  }
}
