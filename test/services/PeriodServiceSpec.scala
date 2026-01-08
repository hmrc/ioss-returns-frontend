/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import base.SpecBase
import models.StandardPeriod

import java.time.Month

class PeriodServiceSpec
  extends SpecBase {


  ".getNextPeriod" - {
    "when current period is January" in {
      val year = 2021
      val current = StandardPeriod(year, Month.JANUARY)
      val expected = StandardPeriod(year, Month.FEBRUARY)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is February" in {
      val year = 2021
      val current = StandardPeriod(year, Month.FEBRUARY)
      val expected = StandardPeriod(year, Month.MARCH)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is March" in {
      val year = 2021
      val current = StandardPeriod(year, Month.MARCH)
      val expected = StandardPeriod(year, Month.APRIL)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is April" in {
      val year = 2021
      val current = StandardPeriod(year, Month.APRIL)
      val expected = StandardPeriod(year, Month.MAY)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is May" in {
      val year = 2021
      val current = StandardPeriod(year, Month.MAY)
      val expected = StandardPeriod(year, Month.JUNE)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is June" in {
      val year = 2021
      val current = StandardPeriod(year, Month.JUNE)
      val expected = StandardPeriod(year, Month.JULY)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is July" in {
      val year = 2021
      val current = StandardPeriod(year, Month.JULY)
      val expected = StandardPeriod(year, Month.AUGUST)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is August" in {
      val year = 2021
      val current = StandardPeriod(year, Month.AUGUST)
      val expected = StandardPeriod(year, Month.SEPTEMBER)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is September" in {
      val year = 2021
      val current = StandardPeriod(year, Month.SEPTEMBER)
      val expected = StandardPeriod(year, Month.OCTOBER)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is October" in {
      val year = 2021
      val current = StandardPeriod(year, Month.OCTOBER)
      val expected = StandardPeriod(year, Month.NOVEMBER)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is November" in {
      val year = 2021
      val current = StandardPeriod(year, Month.NOVEMBER)
      val expected = StandardPeriod(year, Month.DECEMBER)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }

    "when current period is December" in {
      val year = 2021
      val current = StandardPeriod(year, Month.DECEMBER)
      val expected = StandardPeriod(year + 1, Month.JANUARY)
      val service = new PeriodService()
      service.getNextPeriod(current) mustBe expected
    }
  }

  ".fromKey" - {
    "should correctly parse key into StandardPeriod" in {
      val service = new PeriodService()
      val year = 2021

      val key1 = "21AA"
      val expected1 = StandardPeriod(year, Month.JANUARY)
      service.fromKey(key1) mustBe expected1

      val key2 = "21AB"
      val expected2 = StandardPeriod(year, Month.FEBRUARY)
      service.fromKey(key2) mustBe expected2

      val key3 = "21AC"
      val expected3 = StandardPeriod(year, Month.MARCH)
      service.fromKey(key3) mustBe expected3

      val key4 = "21AD"
      val expected4 = StandardPeriod(year, Month.APRIL)
      service.fromKey(key4) mustBe expected4

      val key5 = "21AE"
      val expected5 = StandardPeriod(year, Month.MAY)
      service.fromKey(key5) mustBe expected5

      val key6 = "21AF"
      val expected6 = StandardPeriod(year, Month.JUNE)
      service.fromKey(key6) mustBe expected6

      val key7 = "21AG"
      val expected7 = StandardPeriod(year, Month.JULY)
      service.fromKey(key7) mustBe expected7

      val key8 = "21AH"
      val expected8 = StandardPeriod(year, Month.AUGUST)
      service.fromKey(key8) mustBe expected8

      val key9 = "21AI"
      val expected9 = StandardPeriod(year, Month.SEPTEMBER)
      service.fromKey(key9) mustBe expected9

      val key10 = "21AJ"
      val expected10 = StandardPeriod(year, Month.OCTOBER)
      service.fromKey(key10) mustBe expected10

      val key11 = "21AK"
      val expected11 = StandardPeriod(year, Month.NOVEMBER)
      service.fromKey(key11) mustBe expected11

      val key12 = "21AL"
      val expected12 = StandardPeriod(year, Month.DECEMBER)
      service.fromKey(key12) mustBe expected12
    }
  }

  ".fromEtmpMonthString" - {
    "should correctly map ETMP month string to Month enum" in {
      val service = new PeriodService()

      service.fromEtmpMonthString("AA") mustBe Month.JANUARY
      service.fromEtmpMonthString("AB") mustBe Month.FEBRUARY
      service.fromEtmpMonthString("AC") mustBe Month.MARCH
      service.fromEtmpMonthString("AD") mustBe Month.APRIL
      service.fromEtmpMonthString("AE") mustBe Month.MAY
      service.fromEtmpMonthString("AF") mustBe Month.JUNE
      service.fromEtmpMonthString("AG") mustBe Month.JULY
      service.fromEtmpMonthString("AH") mustBe Month.AUGUST
      service.fromEtmpMonthString("AI") mustBe Month.SEPTEMBER
      service.fromEtmpMonthString("AJ") mustBe Month.OCTOBER
      service.fromEtmpMonthString("AK") mustBe Month.NOVEMBER
      service.fromEtmpMonthString("AL") mustBe Month.DECEMBER
    }
  }
}
