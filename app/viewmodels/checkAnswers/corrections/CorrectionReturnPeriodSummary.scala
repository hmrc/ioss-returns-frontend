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

import models.UserAnswers
import pages.corrections.VatPeriodCorrectionsListPage
import pages.{CheckAnswersPage, Waypoints}
import play.api.i18n.Messages
import queries.AllCorrectionPeriodsQuery
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CorrectionReturnPeriodSummary {

  def getAllRows(answers: UserAnswers, waypoints: Waypoints, sourcePage: CheckAnswersPage)(implicit messages: Messages): Option[SummaryListRow] = {
    val periods =
      answers.get(AllCorrectionPeriodsQuery).getOrElse(List.empty).map {
        correction => correction.correctionReturnPeriod.displayText
      }

    val period = answers.period

    if(periods.nonEmpty) {
      Some(SummaryListRowViewModel(
        key = messages("checkYourAnswers.correctionLabel.l2"),
        value = ValueViewModel(HtmlContent(periods.mkString("</br>")))
          .withCssClass("govuk-summary-list__value  govuk-table__cell--numeric govuk-!-padding-right-9"),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            VatPeriodCorrectionsListPage(period, addAnother = true).changeLink(waypoints, sourcePage).url
          ).withVisuallyHiddenText(messages("correctionReturnPeriod.change.hidden"))
            .withAttribute(("id", "change-correction-periods"))
        )
      ))
    } else {
      None
    }
  }
}
