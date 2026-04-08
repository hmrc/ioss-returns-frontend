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

package viewmodels.checkAnswers.corrections

import models.{Index, UserAnswers}
import pages.corrections.VatAmountCorrectionCountryPage
import pages.{AddItemPage, Waypoints}
import play.api.i18n.Messages
import queries.AllCorrectionCountriesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import uk.gov.hmrc.govukfrontend.views.Aliases.Card
import viewmodels.govuk.all.currencyFormat
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object CorrectionListCountriesSummary {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, periodIndex: Index, sourcePage: AddItemPage)
                   (implicit messages: Messages): Seq[SummaryList] = {

    answers.get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(List.empty).zipWithIndex.flatMap {
      case (correctionToCountry, countryIndex) =>

        val countryName = correctionToCountry.correctionCountry.name

        val value = correctionToCountry.countryVatCorrection.map {
          vatCorrectionAmount =>
            ValueViewModel(
              HtmlContent(
                currencyFormat(vatCorrectionAmount)
              )
            )
        }.getOrElse(ValueViewModel(HtmlContent("")))

        val mainRow = SummaryListRowViewModel(
          key = messages("correctionListCountries.checkYourAnswersLabel"),
          value = value,
          actions = List(ActionItemViewModel(
            messages("site.change"), VatAmountCorrectionCountryPage(periodIndex, Index(countryIndex))
              .changeLink(waypoints, sourcePage).url)
            .withVisuallyHiddenText(messages("correctionListCountries.change.hidden", countryName))
          )
        )

        val rows = List(mainRow)

        List(
          SummaryList(rows)
            .withCard(
              card = Card(
                title = Some(CardTitle(content = HtmlContent(countryName))),
                actions = Some(Actions(
                  items = List(
                    ActionItemViewModel(messages("site.remove"), controllers.corrections.routes.RemoveCountryCorrectionController
                      .onPageLoad(waypoints, periodIndex, Index(countryIndex)).url)
                      .withVisuallyHiddenText(messages("correctionListCountries.remove.hidden", countryName))
                  )
                ))
              )
            )
        )
    }
  }
}
