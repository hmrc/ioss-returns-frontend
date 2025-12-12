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

package pages

import models.{CheckMode, Index, NormalMode}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class WaypointSpec extends AnyFreeSpec with Matchers with OptionValues {

  "fromString" - {

    "must return Check Sales when given it's Normal mode waypoint" in {
      Waypoint.fromString("check-sales-1").value mustBe CheckSalesPage(Index(0)).waypoint(NormalMode)
    }

    "must return Check Sales when given it's Check mode waypoint" in {
      Waypoint.fromString("change-check-sales-1").value mustBe CheckSalesPage(Index(0)).waypoint(CheckMode)
    }

    "must return Sold To Country List when given it's Normal mode waypoint" in {
      Waypoint.fromString("add-sales-country-list").value mustBe SoldToCountryListPage().waypoint(NormalMode)
    }

    "must return Sold To Country List when given it's Check mode waypoint" in {
      Waypoint.fromString("change-add-sales-country-list").value mustBe SoldToCountryListPage().waypoint(CheckMode)
    }

    "must return Check Your Answers when given its waypoint" in {
      Waypoint.fromString("check-your-answers").value mustBe CheckYourAnswersPage.waypoint
    }
  }
}
