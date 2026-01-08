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

import models.TotalVatToCountry
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.CurrencyFormatter
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object CorrectionNoPaymentDueSummary {

  def row(noPaymentDueAmounts: List[TotalVatToCountry])(implicit messages: Messages): Seq[SummaryListRow] = {
    noPaymentDueAmounts.map { totalVatToCountry =>

      val value = ValueViewModel(
        HtmlContent(
          CurrencyFormatter.currencyFormat(totalVatToCountry.totalVat)
        )
      )

      SummaryListRowViewModel(
        key = Key(totalVatToCountry.country.name),
        value = value
      )
    }
  }
}
