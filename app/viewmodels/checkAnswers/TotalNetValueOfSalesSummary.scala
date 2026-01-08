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

package viewmodels.checkAnswers

import models.UserAnswers
import pages.{CheckAnswersPage, SoldToCountryListPage, Waypoints}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalNetValueOfSalesSummary extends CurrencyFormatter {

  def row(answers: UserAnswers, totalNetValueOfSalesOption: Option[BigDecimal], waypoints: Waypoints, sourcePage: CheckAnswersPage)
         (implicit messages: Messages): Option[SummaryListRow] = {
    totalNetValueOfSalesOption.map {
      totalNetValueOfSalesOption =>
        SummaryListRowViewModel(
          key = "checkYourAnswers.label.netValueOfSales",
          value = ValueViewModel(HtmlContent(currencyFormatWithAccuracy(totalNetValueOfSalesOption)))
            .withCssClass("govuk-table__cell--numeric")
            .withCssClass("govuk-!-padding-right-9"),
          actions = Seq(
            ActionItemViewModel("site.change", SoldToCountryListPage().changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("checkYourAnswers.netValueOfSales.hidden"))
              .withAttribute(("id", "change-sales-excluding-vat-eu"))

          )
        )
    }
  }
}
