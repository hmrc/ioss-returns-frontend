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

package services

import models.{Period, StandardPeriod}

import java.time.{Clock, LocalDate, Month}
import javax.inject.Inject

class PeriodService @Inject()(clock: Clock) {

  def getReturnPeriods(commencementDate: LocalDate): Seq[StandardPeriod] =
    getAllPeriods.filterNot(_.lastDay.isBefore(commencementDate))

  def getAllPeriods: Seq[StandardPeriod] = {
    val firstPeriod = StandardPeriod(2021, Month.JANUARY)
    getPeriodsUntilDate(firstPeriod, LocalDate.now(clock))
  }

  private def getPeriodsUntilDate(currentPeriod: StandardPeriod, endDate: LocalDate): Seq[StandardPeriod] = {
    if(currentPeriod.lastDay.isBefore(endDate)) {
      Seq(currentPeriod) ++ getPeriodsUntilDate(getNextPeriod(currentPeriod), endDate)
    } else {
      Seq.empty
    }
  }

  def getNextPeriod(currentPeriod: Period): StandardPeriod = {
    currentPeriod.month match {
      case Month.DECEMBER =>
        StandardPeriod(currentPeriod.year + 1, Month.JANUARY)
      case Month.NOVEMBER =>
        StandardPeriod(currentPeriod.year, Month.DECEMBER)
      case Month.OCTOBER =>
        StandardPeriod(currentPeriod.year, Month.NOVEMBER)
      case Month.SEPTEMBER =>
        StandardPeriod(currentPeriod.year, Month.OCTOBER)
      case Month.AUGUST =>
        StandardPeriod(currentPeriod.year, Month.SEPTEMBER)
      case Month.JULY =>
        StandardPeriod(currentPeriod.year, Month.AUGUST)
      case Month.JUNE =>
        StandardPeriod(currentPeriod.year, Month.JULY)
      case Month.MAY =>
        StandardPeriod(currentPeriod.year, Month.JUNE)
      case Month.APRIL =>
        StandardPeriod(currentPeriod.year, Month.MAY)
      case Month.MARCH =>
        StandardPeriod(currentPeriod.year, Month.APRIL)
      case Month.FEBRUARY =>
        StandardPeriod(currentPeriod.year, Month.MARCH)
      case Month.JANUARY =>
        StandardPeriod(currentPeriod.year, Month.FEBRUARY)
    }
  }

  def fromKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val month = key.drop(2)
    StandardPeriod(s"20$yearLast2".toInt, fromEtmpMonthString(month))
  }

  private def fromEtmpMonthString(keyMonth: String): Month = {
    keyMonth match {
      case "AA" => Month.JANUARY
      case "AB" => Month.FEBRUARY
      case "AC" => Month.MARCH
      case "AD" => Month.APRIL
      case "AE" => Month.MAY
      case "AF" => Month.JUNE
      case "AG" => Month.JULY
      case "AH" => Month.AUGUST
      case "AI" => Month.SEPTEMBER
      case "AJ" => Month.OCTOBER
      case "AK" => Month.NOVEMBER
      case "AL" => Month.DECEMBER
    }
  }
}
