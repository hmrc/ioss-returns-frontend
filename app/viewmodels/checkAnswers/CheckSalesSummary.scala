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

package viewmodels.checkAnswers

import models.{Index, UserAnswers}
import pages.{CheckSalesPage, DeleteVatRateSalesForCountryPage, Waypoints}
import play.api.i18n.Messages
import queries.SalesByCountryQuery
import uk.gov.hmrc.govukfrontend.views.Aliases.CardTitle
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Card, SummaryList}
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CheckSalesSummary {

  def rows(answers: UserAnswers, waypoints: Waypoints, countryIndex: Index)(implicit messages: Messages): Seq[SummaryList] =

    answers.get(SalesByCountryQuery(countryIndex)).toList.flatMap { salesByCountryDetails =>
      salesByCountryDetails.vatRatesFromCountry.toList.flatMap {
        vatRatesFromCountry =>

          vatRatesFromCountry.zipWithIndex.map {
            case (vatRateFromCountry, vatRateIndex) =>

              val rows = SalesToCountrySummary
                .row(answers, waypoints, countryIndex, Index(vatRateIndex), vatRateFromCountry, CheckSalesPage(countryIndex, Some(Index(vatRateIndex)))).toList ++
                VatOnSalesSummary
                  .row(answers, waypoints, countryIndex, Index(vatRateIndex), CheckSalesPage(countryIndex, Some(Index(vatRateIndex)))).toList

              SummaryListViewModel(
                rows = rows
              ).withCard(
                card = Card(
                  title = Some(CardTitle(content = HtmlContent(messages("checkSales.vatRate", vatRateFromCountry.rate)))),
                  actions = Some(Actions(
                    items = Seq(
                      ActionItemViewModel("site.remove", DeleteVatRateSalesForCountryPage(countryIndex, Index(vatRateIndex)).route(waypoints).url)
                        .withVisuallyHiddenText(messages("salesToCountry.remove.hidden", vatRateFromCountry.rateForDisplay)))
                  ))
                )
              )
          }
      }
    }
}
