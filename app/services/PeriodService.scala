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

import models.Period

import java.time.Month
import javax.inject.Inject

class PeriodService @Inject() {

  def getNextPeriod(currentPeriod: Period): Period = {
    currentPeriod.month match {
      case Month.DECEMBER =>
        Period(currentPeriod.year + 1, Month.JANUARY)
      case Month.NOVEMBER =>
        Period(currentPeriod.year, Month.DECEMBER)
      case Month.OCTOBER =>
        Period(currentPeriod.year, Month.NOVEMBER)
      case Month.SEPTEMBER =>
        Period(currentPeriod.year, Month.OCTOBER)
      case Month.AUGUST =>
        Period(currentPeriod.year, Month.SEPTEMBER)
      case Month.JULY =>
        Period(currentPeriod.year, Month.AUGUST)
      case Month.JUNE =>
        Period(currentPeriod.year, Month.JULY)
      case Month.MAY =>
        Period(currentPeriod.year, Month.JUNE)
      case Month.APRIL =>
        Period(currentPeriod.year, Month.MAY)
      case Month.MARCH =>
        Period(currentPeriod.year, Month.APRIL)
      case Month.FEBRUARY =>
        Period(currentPeriod.year, Month.MARCH)
      case Month.JANUARY =>
        Period(currentPeriod.year, Month.FEBRUARY)
    }
  }

}
