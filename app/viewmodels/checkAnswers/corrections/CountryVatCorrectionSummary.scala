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
import pages.Waypoints
import pages.corrections.VatAmountCorrectionCountryPage
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.currencyFormat
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CountryVatCorrectionSummary {

  def row(answers: UserAnswers, waypoints: Waypoints, periodIndex: Index, countryIndex: Index)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(VatAmountCorrectionCountryPage(periodIndex, countryIndex)).map {
      answer =>

        SummaryListRowViewModel(
          key = "vatAmountCorrectionCountry.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(Html(currencyFormat(answer)))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              controllers.corrections.routes.VatAmountCorrectionCountryController.onPageLoad(
                waypoints, periodIndex, countryIndex
              ).url
            ).withVisuallyHiddenText(messages("vatAmountCorrectionCountry.change.hidden"))
              .withAttribute(("id", "change-correction-amount"))
          )
        )
    }

  }
}
