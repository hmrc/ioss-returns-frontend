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

package viewmodels

import base.SpecBase
import models.StandardPeriod
import models.payments.{Payment, PaymentStatus}

import java.time.{LocalDate, Month}

class PaymentsViewModelSpec extends SpecBase {

  private val paymentDue = Payment(period, BigDecimal(1000), LocalDate.now, PaymentStatus.Unpaid)
  val period1: StandardPeriod = StandardPeriod(2021, Month.JULY)
  val period2: StandardPeriod = StandardPeriod(2021, Month.OCTOBER)
  val period3: StandardPeriod = StandardPeriod(2022, Month.JANUARY)

  "must return correct view model when" - {
    val app = applicationBuilder().build()

    "there is no payments due or overdue" in {
      val result = PaymentsViewModel(Seq.empty, Seq.empty, stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(PaymentsSection(Seq("You do not owe anything right now.")))
      result.link must not be defined
      result.warning must not be defined
    }

    "there is one due payment" in {
      val result = PaymentsViewModel(Seq(paymentDue), Seq.empty, stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period.displayShortText}. You must pay this by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Due Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one due payment with unknown status" in {
      val result = PaymentsViewModel(Seq(paymentDue.copy(paymentStatus = PaymentStatus.Unknown)), Seq.empty, stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You may still owe VAT for ${period.displayShortText}. You must pay this by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Due Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one overdue payment" in {
      val result = PaymentsViewModel(Seq.empty, Seq(paymentDue), stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period.displayShortText}, which was due by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Overdue Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one overdue payment with unknown status" in {
      val result = PaymentsViewModel(Seq.empty, Seq(paymentDue.copy(paymentStatus = PaymentStatus.Unknown)), stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(PaymentsSection(
        Seq(
          s"""You may still owe VAT for ${period.displayShortText}, which was due by ${period.paymentDeadlineDisplay}."""
        ),
        Some("Overdue Payments")
      ))
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one due payment, and two overdue payments, one with unknown status" in {
      val result = PaymentsViewModel(Seq(paymentDue.copy(period = period3)), Seq(paymentDue.copy(period = period1, paymentStatus = PaymentStatus.Unknown), paymentDue.copy(period = period2)), stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(
        PaymentsSection(
          Seq(
            s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period3.displayShortText}. You must pay this by ${period3.paymentDeadlineDisplay}.""",
          ),
          Some("Due Payments")),
        PaymentsSection(
          Seq(
            s"""You may still owe VAT for ${period1.displayShortText}, which was due by ${period1.paymentDeadlineDisplay}.""",
            s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period2.displayShortText}, which was due by ${period2.paymentDeadlineDisplay}."""
          ),
          Some("Overdue Payments")
        )
      )
      result.link mustBe defined
      result.warning mustBe defined
    }

    "there is one overdue payment older than three years and one payment overdue" in {
      val paymentOverdueOlderThan3Years = paymentDue.copy(dateDue = arbitraryDate.minusYears(4))
      val result = PaymentsViewModel(Seq.empty, Seq(paymentOverdueOlderThan3Years, paymentDue), stubClockAtArbitraryDate)(messages(app))
      result.sections mustBe Seq(
        PaymentsSection(
          Seq(
            s"You have an outstanding IOSS VAT payment for ${period.displayShortText}. You must contact the countries where you made your sales to pay the VAT due."
          ),
          None
        ),
        PaymentsSection(
          Seq(
            s"""You owe <span class="govuk-body govuk-!-font-weight-bold">&pound;1,000</span> for ${period.displayShortText}, which was due by ${period.paymentDeadlineDisplay}."""
          ),
          Some("Overdue Payments")
        ))
      result.link mustBe defined
      result.warning mustBe defined
    }
  }

}
