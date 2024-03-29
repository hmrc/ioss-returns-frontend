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

package models

import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.{PathBindable, QueryStringBindable}

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

  /*".paymentDeadline" - {

    "must be the 30th of April when the quarter is Q1" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = Period(year, Q1)
          period.paymentDeadline mustEqual LocalDate.of(year, APRIL, 30)
      }
    }

    "must be the 31st of July when the quarter is Q2" in {

      forAll(Gen.choose(2022, 2100)) {
        year =>
          val period = Period(year, Q2)
          period.paymentDeadline mustEqual LocalDate.of(year, JULY, 31)
      }
    }

    "must be the 31st of October when the quarter is Q3" in {

      forAll(Gen.choose(2021, 2100)) {
        year =>
          val period = Period(year, Q3)
          period.paymentDeadline mustEqual LocalDate.of(year, OCTOBER, 31)
      }
    }

    "must be the 31st of January of the following year when the quarter is Q4" in {

      forAll(Gen.choose(2021, 2100)) {
        year =>
          val period = Period(year, Q4)
          period.paymentDeadline mustEqual LocalDate.of(year + 1, JANUARY, 31)
      }
    }
  }

  ".isOverdue" - {

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
}
