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

import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import models.payments.{Payment, PaymentStatus}
import play.api.i18n.Messages
import utils.CurrencyFormatter.currencyFormat

import java.time.Clock

case class PaymentsViewModel(sections: Seq[PaymentsSection], warning: Option[String] = None, link: Option[LinkModel] = None)

case class PaymentsSection(contents: Seq[String], heading: Option[String] = None)

object PaymentsViewModel {
  def apply(duePayments: Seq[Payment], overduePayments: Seq[Payment], excludedPayments: Seq[Payment], clock: Clock)
           (implicit messages: Messages): PaymentsViewModel = {
    if (duePayments.isEmpty && overduePayments.isEmpty) {
      PaymentsViewModel(
        sections = Seq(PaymentsSection(
          contents = Seq(messages("yourAccount.payment.nothingOwed"))
        ))
      )
    } else {
      val excludedPaymentsOlderThanThreeYears = excludedPayments.filter(excludedPayment =>
        isOlderThanThreeYears(excludedPayment.dateDue, clock)
      ).sortBy(_.dateDue)

      val excludedPaymentsSection = if (excludedPaymentsOlderThanThreeYears.nonEmpty) {
        Some(PaymentsSection(
          contents = excludedPaymentsOlderThanThreeYears.map(
            excludedPayment => messages("yourAccount.payment.excludedPayment", excludedPayment.period.displayShortText)
          )
        ))
      } else {
        None
      }

      val duePaymentsSection = getPaymentsSection(duePayments, "due")
      val overduePaymentsSection = getPaymentsSection(overduePayments, "overdue")

      PaymentsViewModel(
        sections = Seq(excludedPaymentsSection, duePaymentsSection, overduePaymentsSection).flatten,
        warning = Some(messages("yourAccount.payment.pendingPayments")),
        link = Some(
          LinkModel(
            linkText = messages("yourAccount.payment.makeAPayment"),
            id = "make-a-payment",
            url = controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad().url
          )
        )
      )
    }
  }

  private def getPaymentsSection(payments: Seq[Payment], key: String)(implicit messages: Messages): Option[PaymentsSection] = {
    if (payments.nonEmpty) {
      Some(
        PaymentsSection(
          heading = Some(messages(s"yourAccount.payment.${key}Heading")),
          contents = payments.map(
            payment => payment.paymentStatus match {
              case PaymentStatus.Unknown =>
                messages(
                  s"yourAccount.payment.${key}AmountMaybeOwed",
                  payment.period.displayShortText,
                  payment.period.paymentDeadlineDisplay
                )
              case _ => messages(
                s"yourAccount.payment.${key}AmountOwed",
                currencyFormat(payment.amountOwed),
                payment.period.displayShortText,
                payment.period.paymentDeadlineDisplay
              )
            }
          )
        )
      )
    } else {
      None
    }
  }

}
