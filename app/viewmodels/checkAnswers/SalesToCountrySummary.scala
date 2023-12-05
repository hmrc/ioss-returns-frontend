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
import models.{Index, UserAnswers, VatRateFromCountry}
import pages.{SalesToCountryPage, Waypoints}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object SalesToCountrySummary  {

  def row(answers: UserAnswers, waypoints: Waypoints, countryIndex: Index, vatRateIndex: Index, vatRate: VatRateFromCountry)
         (implicit messages: Messages): Option[SummaryListRow] =
    answers.get(SalesToCountryPage(countryIndex, vatRateIndex)).map {
      answer =>

        SummaryListRowViewModel(
          key     = "salesToCountry.checkYourAnswersLabel",
          value   = ValueViewModel(answer.toString),
          actions = Seq(
            ActionItemViewModel("site.change", routes.SalesToCountryController.onPageLoad(waypoints, countryIndex, vatRateIndex).url)
              .withVisuallyHiddenText(messages("salesToCountry.change.hidden", vatRate.rateForDisplay))
              .withAttribute(("id", s"change-net-value-sales-${vatRate.rate}-percent"))
          )
        )
    }
}
