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

package viewmodels.checkAnswers

import models.RegistrationWrapper
import models.etmp.intermediary.{EtmpCustomerIdentificationLegacy, EtmpCustomerIdentificationNew, EtmpIdType}
import models.requests.DataRequest
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.govukfrontend.views.Aliases.Key
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object BusinessVRNSummary {

  def vrnRow(request: DataRequest[_])(implicit messages: Messages): Option[SummaryListRow] = {

    request.registrationWrapper.registration.customerIdentification match
      case EtmpCustomerIdentificationLegacy(vrn) =>
        Some(SummaryListRowViewModel(
          key = Key("checkYourAnswers.label.businessVrn").withCssClass("govuk-!-width-one-third"),
          value = ValueViewModel(HtmlFormat.escape(request.vrnOrError.vrn).toString).withCssClass("govuk-table__cell--numeric"),
          actions = Seq.empty
        ))
      case EtmpCustomerIdentificationNew(idType, idValue) =>
        idType match
          case EtmpIdType.VRN =>
            Some(SummaryListRowViewModel(
              key = Key("checkYourAnswers.label.businessVrn").withCssClass("govuk-!-width-one-third"),
              value = ValueViewModel(HtmlFormat.escape(idValue).toString).withCssClass("govuk-table__cell--numeric"),
              actions = Seq.empty
            ))
          case EtmpIdType.UTR | EtmpIdType.FTR =>
            Some(SummaryListRowViewModel(
              key = Key("checkYourAnswers.label.businessUtrFtr").withCssClass("govuk-!-width-one-third"),
              value = ValueViewModel(HtmlFormat.escape(idValue).toString).withCssClass("govuk-table__cell--numeric"),
              actions = Seq.empty
            ))
          case EtmpIdType.NINO =>
            Some(SummaryListRowViewModel(
              key = Key("checkYourAnswers.label.businessNino").withCssClass("govuk-!-width-one-third"),
              value = ValueViewModel(HtmlFormat.escape(idValue).toString).withCssClass("govuk-table__cell--numeric"),
              actions = Seq.empty
            ))
  }
}
