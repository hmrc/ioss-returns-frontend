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
import pages.{AddItemPage, JourneyRecoveryPage, Waypoints}
import play.twirl.api.HtmlFormat
import queries.AllCorrectionCountriesQuery
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem

object CorrectionListCountriesSummary  {

  def addToListRows(answers: UserAnswers, waypoints: Waypoints, periodIndex: Index, sourcePage: AddItemPage): Seq[ListItem] =
    answers.get(AllCorrectionCountriesQuery(answers.period)).getOrElse(List.empty).zipWithIndex.map {
      case (correctionToCountry, countryIndex) =>

        ListItem(
          name = correctionToCountry.correctionCountry.name,
          changeUrl = JourneyRecoveryPage.changeLink(waypoints, sourcePage).url, //TODO navigate to correct page when created
          removeUrl = JourneyRecoveryPage.route(waypoints).url //TODO navigate to correct page when created
        )
    }
}
