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

package models

import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}

import java.time.{LocalDate, Month}

class PeriodSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with EitherValues {

  private val pathBindable = implicitly[PathBindable[Period]]
  private val queryBindable = implicitly[QueryStringBindable[Period]]

  "Period" - {
    "pathBindable" - {
      "must bind from a URL" in {

        forAll(arbitrary[Period]) {
          period =>

            pathBindable.bind("key", period.toString).value mustEqual period
        }
      }

      "must not bind from an invalid value" in {

        pathBindable.bind("key", "invalid").left.value mustEqual "Invalid period"
      }
    }

    "queryBindable" - {
      "must bind from a query parameter when valid period present" in {

        forAll(arbitrary[Period]) {
          period =>

            queryBindable.bind("key", Map("key" -> Seq(period.toString))) mustBe Some(Right(period))
        }
      }

      "must not bind from an invalid value" in {

        queryBindable.bind("key", Map("key" -> Seq("invalid"))) mustBe Some(Left("Invalid period"))
      }

      "must return none if no query parameter present" in {
         queryBindable.bind("key", Map("key" -> Seq.empty)) mustBe None
      }
    }


  }

  ".firstDay" - {

    "must be the first of January when the Month is January" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.JANUARY)
          period.firstDay mustEqual LocalDate.of(year, Month.JANUARY, 1)
      }
    }

    "must be the first of April when the Month is April" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.APRIL)
          period.firstDay mustEqual LocalDate.of(year, Month.APRIL, 1)
      }
    }
  }

  ".lastDay" - {

    "must be the 31st of March when the Month is March" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.MARCH)
          period.lastDay mustEqual LocalDate.of(year, Month.MARCH, 31)
      }
    }

    "must be the 30th of June when the Month is June" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.JUNE)
          period.lastDay mustEqual LocalDate.of(year, Month.JUNE, 30)
      }
    }

  }

  ".paymentDeadline" - {

    "must be the 30th of April when the month is March" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.MARCH)
          period.paymentDeadline mustEqual LocalDate.of(year, Month.APRIL, 30)
      }
    }

    "must be the 31st of July when the Month is June" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.JUNE)
          period.paymentDeadline mustEqual LocalDate.of(year, Month.JULY, 31)
      }
    }

    "must be the 31st of October when the Month is September" in {

      forAll(Gen.choose(2021, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.SEPTEMBER)
          period.paymentDeadline mustEqual LocalDate.of(year, Month.OCTOBER, 31)
      }
    }

    "must be the 31st of January of the following year when the month is December" in {

      forAll(Gen.choose(2021, 2100)) {
        year =>
          val period = StandardPeriod(year, Month.DECEMBER)
          period.paymentDeadline mustEqual LocalDate.of(year + 1, Month.JANUARY, 31)
      }
    }
  }

  /*".isOverdue" - {

    "returns true when" - {

      "period is 2021-Q3 and today is 1st December 2021" in {
        val period = Period(2021, Q3)
        val clock: Clock = Clock.fixed(Instant.parse("2021-12-01T12:00:00Z"), ZoneId.systemDefault)

        period.isOverdue(clock) mustBe true
      }

    }

    "returns false when" - {

      "period is 2021-Q3 and today is 25th October" in {
        val period = Period(2021, Q3)
        val clock: Clock = Clock.fixed(Instant.parse("2021-10-25T12:00:00Z"), ZoneId.systemDefault)

        period.isOverdue(clock) mustBe false
      }

    }

  }*/

  "return the correct display text for a StandardPeriod" in {

    val period = StandardPeriod(2023, Month.JANUARY)

    val displayText = period.displayText

    displayText mustBe "January 2023"
  }

  "correctly calculate the next period" in {

    val period = StandardPeriod(2023, Month.DECEMBER)

    val nextPeriod = period.getNext

    nextPeriod mustBe StandardPeriod(2024, Month.JANUARY)
  }

  "correctly format the payment deadline display" in {

    val period = StandardPeriod(2023, Month.JANUARY)

    val paymentDeadline = period.paymentDeadlineDisplay

    paymentDeadline mustBe "28 February 2023"
  }

  "return the correct short display text" in {

    val period = StandardPeriod(2023, Month.JANUARY)

    val shortText = period.displayShortText

    shortText mustBe "January 2023"
  }

  "return the correct partial period last day text" in {

    val period = StandardPeriod(2023, Month.JANUARY)

    val shortText = period.displayPartialPeriodLastDayText

    shortText mustBe "31 January 2023"
  }

  "return the correct to and from text" in {

    val period = StandardPeriod(2023, Month.JANUARY)

    val shortText = period.displayToAndFromText

    shortText mustBe "1 to 31 January 2023"
  }

  "return true when comparing if one period is before another" in {

    val period1 = StandardPeriod(2023, Month.JANUARY)
    val period2 = StandardPeriod(2023, Month.FEBRUARY)

    val isBefore = period1.isBefore(period2)

    isBefore mustBe true
  }

  "parse a valid period string" in {

    val periodString = "2023-M1"

    val period = Period.fromString(periodString)

    period mustBe Some(StandardPeriod(2023, Month.JANUARY))
  }

  "return None for an invalid period string" in {

    val invalidString = "invalid"

    val period = Period.fromString(invalidString)

    period mustBe None
  }

  "generate the correct sequence of RadioItem instances" in {

    val periods = Seq(
      StandardPeriod(2023, Month.JANUARY),
      StandardPeriod(2023, Month.FEBRUARY),
      StandardPeriod(2023, Month.MARCH)
    )

    val radioItems = StandardPeriod.options(periods)

    radioItems must have size 3

    radioItems.head mustBe RadioItem(
      content = Text("January 2023"),
      value = Some("2023-M1"),
      id = Some("value_0")
    )

    radioItems(1) mustBe RadioItem(
      content = Text("February 2023"),
      value = Some("2023-M2"),
      id = Some("value_1")
    )

    radioItems(2) mustBe RadioItem(
      content = Text("March 2023"),
      value = Some("2023-M3"),
      id = Some("value_2")
    )
  }

  "return an empty sequence if no periods are provided" in {

    val periods = Seq.empty[StandardPeriod]

    val radioItems = StandardPeriod.options(periods)

    radioItems mustBe empty
  }

  "return Some(StandardPeriod) for a valid string" in {

    val validString = "2023-M1"

    val result = StandardPeriod.fromString(validString)

    result mustBe Some(StandardPeriod(2023, Month.JANUARY))
  }

  "return None for an invalid string" in {

    val invalidString = "invalid"

    val result = StandardPeriod.fromString(invalidString)

    result mustBe None
  }

  "return None for a string with an invalid format" in {

    val invalidFormatString = "2023-01"

    val result = StandardPeriod.fromString(invalidFormatString)

    result mustBe None
  }

  "return None for a string with an out-of-range month" in {

    val outOfRangeMonthString = "2023-M13"

    val result = StandardPeriod.fromString(outOfRangeMonthString)

    result mustBe None
  }
}
