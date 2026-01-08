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

package testUtils

import base.SpecBase
import models.Period
import models.etmp.EtmpObligationDetails
import models.payments.{Payment, PrepareData}
import org.scalacheck.Gen

object PeriodWithFinancialData extends SpecBase {

  val obligationDetails: Seq[EtmpObligationDetails] =
    Gen.listOfN(5, arbitraryObligationDetails.arbitrary).sample.value

  val obligationPeriods: Seq[Period] = obligationDetails.map(_.periodKey).map(Period.fromKey)

  val payments: List[Payment] = obligationPeriods.map { period =>
    arbitraryPayment.arbitrary.sample.value.copy(period = period)
  }.toList

  val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = obligationPeriods.flatMap { period =>
    Map(period -> payments.filter(_.period == period).head)
  }.groupBy(_._1.year)

  val prepareData: PrepareData = {
    PrepareData(
      duePayments = List(payments.head),
      overduePayments = payments.tail,
      excludedPayments = List.empty,
      totalAmountOwed = payments.map(_.amountOwed).sum,
      totalAmountOverdue = BigDecimal(0),
      iossNumber = iossNumber
    )
  }

  val emptyPrepareData: PrepareData = {
    PrepareData(
      duePayments = List.empty,
      overduePayments = List.empty,
      excludedPayments = List.empty,
      totalAmountOwed = BigDecimal(0),
      totalAmountOverdue = BigDecimal(0),
      iossNumber = iossNumber
    )
  }
}
