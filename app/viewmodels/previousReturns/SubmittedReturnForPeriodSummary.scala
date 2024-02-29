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

package viewmodels.previousReturns

import models.Period
import models.etmp.EtmpVatReturn
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Formatters.etmpDateFormatter
import viewmodels.govuk.all.currencyFormat
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SubmittedReturnForPeriodSummary {

  def rowVatDeclared(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    val value = etmpVatReturn.totalVATAmountDueForAllMSGBP

    Some(SummaryListRowViewModel(
      key = "submittedReturnForPeriod.summary.vatDeclared",
      value = ValueViewModel(HtmlContent(currencyFormat(value))).withCssClass("govuk-table__cell--numeric")
    ))
  }

  def rowRemainingAmount(outstandingAmount: Option[BigDecimal])(implicit messages: Messages): Option[SummaryListRow] = {
    outstandingAmount.map { amount =>
      SummaryListRowViewModel(
        key = "submittedReturnForPeriod.summary.remainingAmount",
        value = ValueViewModel(HtmlContent(currencyFormat(amount))).withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
      )
    }
  }

  def rowReturnSubmittedDate(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    val value = etmpVatReturn.returnVersion

    Some(SummaryListRowViewModel(
      key = "submittedReturnForPeriod.summary.submittedDate",
      value = ValueViewModel(HtmlContent(value.format(etmpDateFormatter))).withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowPaymentDueDate(period: Period)(implicit messages: Messages): Option[SummaryListRow] = {
    val value = period.paymentDeadline

    Some(SummaryListRowViewModel(
      key = "submittedReturnForPeriod.summary.paymentDueDate",
      value = ValueViewModel(HtmlContent(value.format(etmpDateFormatter))).withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowReturnReference(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    val value = etmpVatReturn.returnReference

    Some(SummaryListRowViewModel(
      key = "submittedReturnForPeriod.summary.returnReference",
      value = ValueViewModel(HtmlContent(value)).withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }

  def rowPaymentReference(etmpVatReturn: EtmpVatReturn)(implicit messages: Messages): Option[SummaryListRow] = {
    val value = etmpVatReturn.paymentReference

    Some(SummaryListRowViewModel(
      key = "submittedReturnForPeriod.summary.paymentReference",
      value = ValueViewModel(HtmlContent(value)).withCssClass("govuk-table__cell--numeric govuk-!-padding-right-0")
    ))
  }
}
