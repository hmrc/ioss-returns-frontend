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

import base.SpecBase
import connectors.ReturnStatusConnector
import models.{Period, StandardPeriod}
import models.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import models.requests.OptionalDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import viewmodels.yourAccount.{CurrentReturns, Return}

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckIsCurrentReturnPeriodFilterImplSpec extends SpecBase with BeforeAndAfterEach with TableDrivenPropertyChecks {

  private val earliestPeriod: StandardPeriod = StandardPeriod(2021, Month.JULY)
  private val middlePeriod: StandardPeriod = StandardPeriod(2021, Month.OCTOBER)
  private val latestPeriod: StandardPeriod = StandardPeriod(2022, Month.JANUARY)

  private val mockReturnStatusConnector: ReturnStatusConnector = mock[ReturnStatusConnector]

  class Harness(startReturnPeriod: Period) extends CheckIsCurrentReturnPeriodFilterImpl(startReturnPeriod, mockReturnStatusConnector) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  override protected def beforeEach(): Unit =
    Mockito.reset(mockReturnStatusConnector)

  "filter should" - {
    "prioritise first overdue over due, even if due is earlier" in {
      val returns = Seq(
        Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false),
        Return.fromPeriod(earliestPeriod, Due, inProgress = false, isOldest = false)
      )

      when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
        .thenReturn(Future.successful(Right(
          CurrentReturns(returns = returns, excluded = false, finalReturnsCompleted = false, completeOrExcludedReturns = List.empty)
        )))

      val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
      val controller = new Harness(middlePeriod)

      val result = controller.callFilter(request).futureValue
      result mustBe None
    }

    "redirect to SubmittedReturnForPeriodController when it is a complete return" in {
      val activeReturnOptions = Table(
        "active returns",
        List.empty,
        Seq(
          Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true)
        )
      )

      val completeOrExcludedReturns = Seq(
        Return.fromPeriod(period = StandardPeriod(middlePeriod.year, Month.JANUARY), submissionStatus = Excluded, inProgress = false, isOldest = false),
        Return.fromPeriod(period = middlePeriod, submissionStatus = Complete, inProgress = false, isOldest = false),
        Return.fromPeriod(period = earliestPeriod, submissionStatus = Excluded, inProgress = false, isOldest = false)
      )

      forAll(activeReturnOptions) { activeReturnOption =>
        val currentReturns = CurrentReturns(
          returns = activeReturnOption,
          excluded = false,
          finalReturnsCompleted = false,
          completeOrExcludedReturns = completeOrExcludedReturns
        )

        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(Right(currentReturns)))

        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
        val controller = new Harness(middlePeriod)

        val result = controller.callFilter(request).futureValue
        result mustBe Some(Redirect(controllers.previousReturns.routes.SubmittedReturnForPeriodController.onPageLoad(waypoints, middlePeriod)))
      }
    }

    "redirect to CannotStartExcludedReturnController when it is an excluded return" in {
      val activeReturnOptions = Table(
        "active returns",
        List.empty,
        Seq(
          Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true)
        )
      )

      val completeOrExcludedReturns = Seq(
        Return.fromPeriod(period = StandardPeriod(middlePeriod.year, Month.JANUARY), submissionStatus = Complete, inProgress = false, isOldest = false),
        Return.fromPeriod(period = middlePeriod, submissionStatus = Excluded, inProgress = false, isOldest = false),
        Return.fromPeriod(period = earliestPeriod, submissionStatus = Excluded, inProgress = false, isOldest = false)
      )

      forAll(activeReturnOptions) { activeReturnOption =>

        val currentReturns = CurrentReturns(
          returns = activeReturnOption,
          excluded = false,
          finalReturnsCompleted = false,
          completeOrExcludedReturns = completeOrExcludedReturns
        )

        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(Right(currentReturns)))

        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
        val controller = new Harness(middlePeriod)

        val result = controller.callFilter(request).futureValue
        result mustBe Some(Redirect(controllers.routes.CannotStartExcludedReturnController.onPageLoad()))
      }
    }

    "redirect to SubmittedReturnForPeriodController when it is a next return as it is not valid for current period" in {
      val returns = Seq(
        Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(period = middlePeriod, submissionStatus = Excluded, inProgress = false, isOldest = false),
        Return.fromPeriod(period = earliestPeriod, submissionStatus = Next, inProgress = false, isOldest = false)
      )

      val completeOrExcludedReturns = Seq(
        Return.fromPeriod(period = StandardPeriod(middlePeriod.year, Month.JANUARY), submissionStatus = Complete, inProgress = false, isOldest = false)
      )

      val currentReturns = CurrentReturns(
        returns = returns,
        excluded = false,
        finalReturnsCompleted = false,
        completeOrExcludedReturns = completeOrExcludedReturns
      )

      when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
        .thenReturn(Future.successful(Right(currentReturns)))

      val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
      val controller = new Harness(earliestPeriod)

      val result = controller.callFilter(request).futureValue
      result mustBe Some(Redirect(controllers.routes.NoOtherPeriodsAvailableController.onPageLoad()))
    }

    "fail when first due is not current period" in {
      val unknownPeriod = StandardPeriod(2029, Month.AUGUST)

      val returns = Seq(
        Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true)
      )

      val currentReturns = CurrentReturns(
        returns = returns,
        excluded = false,
        finalReturnsCompleted = false,
        completeOrExcludedReturns = List.empty
      )

      when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
        .thenReturn(Future.successful(Right(currentReturns)))

      val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
      val controller = new Harness(unknownPeriod)

      val result = controller.callFilter(request).futureValue
      result mustBe Some(Redirect(controllers.routes.CannotStartReturnController.onPageLoad()))
    }
  }
}
