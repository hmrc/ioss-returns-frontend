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

package viewmodels.checkAnswers.corrections

import models.{Index, UserAnswers}
import pages.corrections.{RemoveCountryCorrectionPage, VatAmountCorrectionCountryPage}
import pages.{AddItemPage, Waypoints}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import queries.AllCorrectionCountriesQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist._
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import viewmodels.govuk.all.currencyFormat

object CorrectionListCountriesSummary  {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, periodIndex: Index, sourcePage: AddItemPage): Seq[ListItem] =
    answers.get(AllCorrectionCountriesQuery(periodIndex)).getOrElse(List.empty).zipWithIndex.map {
      case (correctionToCountry, countryIndex) =>

        ListItem(
          name = correctionToCountry.correctionCountry.name,
          changeUrl = VatAmountCorrectionCountryPage(periodIndex, Index(countryIndex)).changeLink(waypoints, sourcePage).url,
          removeUrl = controllers.corrections.routes.RemoveCountryCorrectionController.onPageLoad(waypoints, periodIndex, Index(countryIndex)).url
        )
    }
}
