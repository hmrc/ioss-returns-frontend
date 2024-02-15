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

import scala.annotation.tailrec

class CorrectionsService {

  def getAllPeriods(periodFrom: Period, periodTo: Period): Seq[Period] = {

    @tailrec
    def getAllPeriodsInRange(currentPeriods: Seq[Period], periodFrom: Period, periodTo: Period): Seq[Period] = {
      (periodFrom, periodTo) match {
        case (pf, pt) if pf.isBefore(pt) =>
          val updatedPeriod = currentPeriods :+ pf
          getAllPeriodsInRange(updatedPeriod, pf.getNext, pt)
        case _ => currentPeriods
      }
    }

    getAllPeriodsInRange(Seq.empty, periodFrom, periodTo)
  }
}
