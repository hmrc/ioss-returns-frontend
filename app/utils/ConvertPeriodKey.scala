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

package utils

import models.{Period, StandardPeriod}

import java.time.Month
import java.time.Month._

object ConvertPeriodKey {

  def yearFromEtmpPeriodKey(periodKey: String): Int = {
    s"20${periodKey.substring(0, 2)}".toInt
  }

  private def monthFromEtmpPeriodKey(periodKey: String): Month = {
    val monthKey = periodKey.substring(2, 4)
    fromEtmpMonthString(monthKey)
  }

  def monthNameFromEtmpPeriodKey(periodKey: String): String = {
    val month = monthFromEtmpPeriodKey(periodKey)
    month.toString.toLowerCase.capitalize
  }

  def periodkeyToPeriod(periodKey: String): Period = {
    val year = periodKey.substring(0, 2)
    val month = periodKey.substring(2, 4)

    StandardPeriod(s"20$year".toInt, fromEtmpMonthString(month))
  }

  def fromEtmpMonthString(monthKey: String): Month = {
    monthKey match {
      case "AA" => JANUARY
      case "AB" => FEBRUARY
      case "AC" => MARCH
      case "AD" => APRIL
      case "AE" => MAY
      case "AF" => JUNE
      case "AG" => JULY
      case "AH" => AUGUST
      case "AI" => SEPTEMBER
      case "AJ" => OCTOBER
      case "AK" => NOVEMBER
      case "AL" => DECEMBER
    }
  }

}
