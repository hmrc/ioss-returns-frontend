/*
 * Copyright 2024 HM Revenue & Customs
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

package models.payments

import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import viewmodels.govuk.all.currencyFormat

final case class PrepareData(
                              duePayments: List[Payment],
                              overduePayments: List[Payment],
                              excludedPayments: List[Payment],
                              totalAmountOwed: BigDecimal,
                              totalAmountOverdue: BigDecimal,
                              iossNumber: String
                            )

object PrepareData {

  implicit val format: OFormat[PrepareData] = Json.format[PrepareData]

  def options(preparedData: Seq[PrepareData])(implicit messages: Messages): Seq[RadioItem] = {
    preparedData.zipWithIndex.map {
      case (prepareData, index) =>

        RadioItem(
          content = HtmlContent(messages("whichPreviousRegistrationToPay.selection", currencyFormat(prepareData.totalAmountOverdue), prepareData.iossNumber)),
          id = Some(s"value_$index"),
          value = Some(prepareData.iossNumber)
        )
    }
  }
}
