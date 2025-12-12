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

package viewmodels.yourAccount

import base.SpecBase
import models.StandardPeriod
import models.SubmissionStatus._
import play.api.Application

import java.time.Month

class ReturnsViewModelSpec extends SpecBase {

  val app: Application = applicationBuilder().build()
  val year2023 = 2023
  val year2024 = 2024

  val olderThan3YearsPeriod: StandardPeriod = StandardPeriod(arbitraryDate.getYear - 4, Month.JULY)
  val earliestPeriod: StandardPeriod = StandardPeriod(2023, Month.JULY)
  val middlePeriod: StandardPeriod = StandardPeriod(2023, Month.OCTOBER)
  val latestPeriod: StandardPeriod = StandardPeriod(2024, Month.JANUARY)

  "must return correct view model when" - {

    "there is no returns due, multiple returns overdue and none in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))

      assert(resultModel.contents.exists(p => p.content == "You have 2 overdue returns."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is no returns due, multiple returns overdue and none in progress2" in {
      val excludedReturns = Seq.empty
      val returns = List(
        Return.fromPeriod(StandardPeriod(2023, Month.DECEMBER), Overdue, false, false),
        Return.fromPeriod(StandardPeriod(2024, Month.JANUARY), Overdue, false, false),
        Return.fromPeriod(StandardPeriod(2024, Month.FEBRUARY), Due, false, false),
      )

      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))

      assert(resultModel.contents.exists(p => p.content == "You have 2 overdue returns."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your December 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, StandardPeriod(2023, Month.DECEMBER)).url
    }

    "there is no returns due, multiple returns overdue and one in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, true, true),
        Return.fromPeriod(middlePeriod, Overdue, false, false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Continue your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(earliestPeriod).url
    }

    "there is no returns due, one return overdue and none in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, inProgress = false, isOldest = true),
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))

      assert(resultModel.contents.map(p => p.content).contains("Your July 2023 return is due by 31 August 2023."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is overdue, this is prioritised over due" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false),
        Return.fromPeriod(latestPeriod, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."), "Your January 2024 is due by 28 February 2024.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is overdue, this is prioritised over due, even if date wise it is later than the due" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(latestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false),
        Return.fromPeriod(earliestPeriod, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."), "Your January 2024 is due by 28 February 2024.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your October 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, middlePeriod).url
    }

    "there is no returns due, one return overdue and one in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, true, true)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return in progress."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Continue your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(earliestPeriod).url
    }

    "there is one return due, multiple returns overdue and none in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false),
        Return.fromPeriod(latestPeriod, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."), "Your January 2024 is due by 28 February 2024.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is one returns due, multiple returns overdue and one in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, true, true),
        Return.fromPeriod(middlePeriod, Overdue, false, false),
        Return.fromPeriod(latestPeriod, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("Your January 2024 return is due by 29 February 2024."))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Continue your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(earliestPeriod).url
    }

    "there is one returns due, one return overdue and one in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, true, true),
        Return.fromPeriod(middlePeriod, Due, false, false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("Your October 2023 return is due by 30 November 2023."))
      assert(resultModel.contents.map(p => p.content).contains("You also have an overdue return in progress."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Continue your July 2023 return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(earliestPeriod).url
    }

    "there is one returns due, one return overdue and none in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(middlePeriod, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return."), "Your October 2023 return is due by 30 November 2023.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is one returns due, no return overdue and one in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Due, true, true)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains(
          """Your return for July 2023 is in progress.
            |<br>This is due by 31 August 2023.
            |<br>""".stripMargin))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Continue your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.ContinueReturnController.onPageLoad(earliestPeriod).url
    }

    "there is one returns due, no return overdue and none in progress" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Due, inProgress = false, isOldest = true)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("Your July 2023 return is due by 31 August 2023."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, earliestPeriod).url
    }

    "there is no returns due, no return overdue" in {
      val excludedReturns = Seq.empty
      val returns = Seq(
        Return.fromPeriod(earliestPeriod, Next, inProgress = true, isOldest = true)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("""You can complete your next return from <span class="govuk-body govuk-!-font-weight-bold">1 August 2023</span>."""))
      resultModel.linkToStart must not be defined
    }

    "there is one excluded return less than three years old and one return overdue" in {
      val excludedReturns = Seq(Return.fromPeriod(latestPeriod, Excluded, inProgress = false, isOldest = true))
      val returns = Seq(
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      resultModel.contents.map(p => p.content) mustBe Seq("Your October 2023 return is due by 30 November 2023.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, middlePeriod).url
    }

    "there is one excluded return older than three years and one return overdue" in {
      val excludedReturns = Seq(Return.fromPeriod(olderThan3YearsPeriod, Expired, inProgress = false, isOldest = true))
      val returns = Seq(
        Return.fromPeriod(middlePeriod, Overdue, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns, excludedReturns, stubClockAtArbitraryDate)(messages(app))
      resultModel.contents.map(p => p.content) mustBe Seq(
        s"You must complete your ${olderThan3YearsPeriod.displayShortText} return with the countries where you made your sales.",
        "Your October 2023 return is due by 30 November 2023."
      )
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, middlePeriod).url
    }
  }

}
