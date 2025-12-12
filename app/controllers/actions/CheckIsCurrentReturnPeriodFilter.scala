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

package controllers.actions

import connectors.ReturnStatusConnector
import models.{ErrorResponse, Period}
import models.SubmissionStatus.{Complete, Excluded, Expired}
import models.requests.OptionalDataRequest
import pages.EmptyWaypoints
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.yourAccount.{CurrentReturns, NextReturn, NextReturnCalculation, OtherReturn}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckIsCurrentReturnPeriodFilterImpl(startReturnPeriod: Period,
                                           returnStatusConnector: ReturnStatusConnector)
                                          (implicit protected val executionContext: ExecutionContext)
  extends ActionFilter[OptionalDataRequest] {

  override protected def filter[A](request: OptionalDataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    returnStatusConnector.getCurrentReturns(request.iossNumber).map { (currentReturnsResponse: Either[ErrorResponse, CurrentReturns]) =>
      currentReturnsResponse match {
        case Left(value: ErrorResponse) => throw new RuntimeException(s"failed getting current returns: $value")
        case Right(currentReturns: CurrentReturns) => processCurrentReturns(currentReturns)
      }
    }
  }

  private def processCurrentReturns(currentReturns: CurrentReturns): Option[Result] = {
    val exceptionOrReturn = NextReturnCalculation.calculateNonNextReturn(currentReturns.returns)

    exceptionOrReturn match {
      case Left(exception) => throw exception
      case Right(actionableReturn) =>
        actionableReturn match {
          case NextReturn(_) =>
            val redirect = remapCompleteOrExcludedToRedirect(currentReturns)
              .getOrElse(Redirect(controllers.routes.NoOtherPeriodsAvailableController.onPageLoad(EmptyWaypoints)))

            Some(redirect)
          case other: OtherReturn =>
            other.maybeExpectedCurrentReturn match {
              case Some(expectedCurrentReturn) if expectedCurrentReturn.period == startReturnPeriod =>
                None

              case _ =>
                val redirect = remapCompleteOrExcludedToRedirect(currentReturns)
                  .getOrElse(Redirect(controllers.routes.CannotStartReturnController.onPageLoad()))

                Some(redirect)
            }
        }
    }
  }

  private def remapCompleteOrExcludedToRedirect(currentReturns: CurrentReturns): Option[Result] = {
    currentReturns.completeOrExcludedReturns.find(_.period == startReturnPeriod).map {
      foundCompleteOrExcludedReturn =>
        val submissionStatus = foundCompleteOrExcludedReturn.submissionStatus
        submissionStatus match {
          case Complete =>
            Redirect(controllers.previousReturns.routes.SubmittedReturnForPeriodController.onPageLoad(EmptyWaypoints, startReturnPeriod))
          case Excluded | Expired =>
            Redirect(controllers.routes.CannotStartExcludedReturnController.onPageLoad())
          case _ =>
            throw new RuntimeException(s"Unexpected status found in foundCompleteOrExcludedReturn $submissionStatus")
        }
    }
  }
}

class CheckIsCurrentReturnPeriodFilter @Inject()(returnStatusConnector: ReturnStatusConnector)(implicit ec: ExecutionContext) {

  def apply(startReturnPeriod: Period): CheckIsCurrentReturnPeriodFilterImpl =
    new CheckIsCurrentReturnPeriodFilterImpl(startReturnPeriod, returnStatusConnector)
}
