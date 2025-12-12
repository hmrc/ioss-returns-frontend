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

package viewmodels.checkAnswers.corrections

import models.{Index, UserAnswers}
import pages.corrections.VatAmountCorrectionCountryPage
import pages.{AddItemPage, Waypoints}
import play.api.i18n.Messages
import queries.AllCorrectionCountriesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import viewmodels.govuk.all.currencyFormat
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CorrectionListCountriesSummary {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, periodIndex: Index, sourcePage: AddItemPage)
                   (implicit messages: Messages): Seq[SummaryList] = {

    answers.get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(List.empty).zipWithIndex.flatMap {
      case (correctionToCountry, countryIndex) =>

        val name = correctionToCountry.correctionCountry.name

        val value = correctionToCountry.countryVatCorrection.map {
          vatCorrectionAmount =>
            ValueViewModel(
              HtmlContent(
                currencyFormat(vatCorrectionAmount)
              )
            )
        }.getOrElse(ValueViewModel(HtmlContent("")))

        val countryNameRow = SummaryListRowViewModel(
          key = Key(name)
            .withCssClass("govuk-!-font-size-24 govuk-!-width-one-third"),
          value = ValueViewModel(HtmlContent("")),
          actions = List.empty
        )

        val mainRow = SummaryListRowViewModel(
          key = messages("correctionListCountries.checkYourAnswersLabel"),
          value = value,
          actions = List(
            ActionItemViewModel(
              messages("site.change"), VatAmountCorrectionCountryPage(periodIndex, Index(countryIndex))
                .changeLink(waypoints, sourcePage).url)
              .withVisuallyHiddenText(messages("correctionListCountries.change.hidden", name)),
            ActionItemViewModel(messages("site.remove"), controllers.corrections.routes.RemoveCountryCorrectionController
              .onPageLoad(waypoints, periodIndex, Index(countryIndex)).url)
              .withVisuallyHiddenText(messages("correctionListCountries.remove.hidden", name))
          )
        )

        val rows = List(countryNameRow, mainRow)

        List(SummaryList(rows))
    }
  }
}
