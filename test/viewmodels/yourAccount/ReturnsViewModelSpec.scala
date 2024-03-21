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

package viewmodels.yourAccount

import base.SpecBase
import models.StandardPeriod
import models.SubmissionStatus._
import play.api.Application

import java.time.Month

class ReturnsViewModelSpec extends SpecBase{

  val app: Application = applicationBuilder().build()
  val year2021 = 2021
  val year2022 = 2022

  val period1: StandardPeriod = StandardPeriod(year2021, Month.JULY)
  val period2: StandardPeriod = StandardPeriod(year2021, Month.OCTOBER)
  val period3: StandardPeriod = StandardPeriod(year2022, Month.JANUARY)

  "must return correct view model when" - {

    "there is no returns due, multiple returns overdue and none in progress" in {

      val returns = Seq(
        Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(period2, Overdue, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))

      assert(resultModel.contents.exists(p => p.content == "You have 2 overdue returns."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, period1).url
    }

    "there is no returns due, one return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true),
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))

      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, period1).url
    }

    "there is one return due, multiple returns overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(period2, Overdue, inProgress = false, isOldest = false),
        Return.fromPeriod(period3, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have 2 overdue returns."), "Your January 2022 is due by 28 February 2022.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, period1).url
    }

    "there is one returns due, one return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Overdue, inProgress = false, isOldest = true),
        Return.fromPeriod(period2, Due, inProgress = false, isOldest = false)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content).contains("You have an overdue return."), "Your October 2021 is due by 30 November 2021.")
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, period1).url
    }

    "there is one returns due, no return overdue and none in progress" in {
      val returns = Seq(
        Return.fromPeriod(period1, Due, inProgress = false, isOldest = true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("Your July 2021 return is due by 31 August 2021."))
      resultModel.linkToStart mustBe defined
      resultModel.linkToStart.get.linkText mustBe "Start your return"
      resultModel.linkToStart.get.url mustBe controllers.routes.StartReturnController.onPageLoad(waypoints, period1).url
    }

    "there is no returns due, no return overdue" in {
      val returns = Seq(
        Return.fromPeriod(period1, Next, inProgress = true, isOldest = true)
      )
      val resultModel = ReturnsViewModel(returns)(messages(app))
      assert(resultModel.contents.map(p => p.content)
        .contains("""You can complete your next return from <span class="govuk-body govuk-!-font-weight-bold">1 August 2021</span>."""))
      resultModel.linkToStart must not be defined
    }
  }

}
