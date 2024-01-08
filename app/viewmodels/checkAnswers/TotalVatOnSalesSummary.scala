/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.routes
import models.UserAnswers
import pages.Waypoints
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object TotalVatOnSalesSummary extends CurrencyFormatter {

  def row(answers: UserAnswers, totalVatOnSalesOption: Option[BigDecimal], waypoints: Waypoints)(implicit messages: Messages): Option[SummaryListRow] = {
    totalVatOnSalesOption.map {
      totalVatOnSales =>
        SummaryListRowViewModel(
          key = "checkYourAnswers.label.vatOnSales",
          value = ValueViewModel(HtmlContent(currencyFormat(totalVatOnSales))).withCssClass("govuk-table__cell--numeric"),
          actions = Seq(
            ActionItemViewModel("site.change", routes.SoldToCountryListController.onPageLoad(waypoints).url)
              .withVisuallyHiddenText(messages("soldGoodsFromEu.changeEUVAT.hidden"))
              .withAttribute(("id", "change-vat-charged-eu"))

          )
        )
    }
  }
}
