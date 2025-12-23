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

package models.audit

import models.etmp.intermediary.EtmpIdType.VRN
import models.etmp.intermediary.EtmpCustomerIdentificationNew
import models.{UserAnswers, UserAnswersForAudit}
import models.requests.DataRequest
import play.api.libs.json.{JsValue, Json}

case class ReturnsAuditModel(
                              userId: String,
                              userAgent: String,
                              vrn: String,
                              userAnswers: UserAnswersForAudit,
                              submissionResult: SubmissionResult
                            ) extends JsonAuditModel {

  override val auditType: String = "ReturnSubmitted"
  override val transactionName: String = "return-submitted"

  override val detail: JsValue = Json.obj(
    "userId" -> userId,
    "browserUserAgent" -> userAgent,
    "requestersVrn" -> vrn,
    "userAnswersDetails" -> Json.toJson(userAnswers),
    "submissionResult" -> Json.toJson(submissionResult)
  )
}

object ReturnsAuditModel {

  def build(
             userAnswers: UserAnswers,
             submissionResult: SubmissionResult
           )(implicit request: DataRequest[_]): ReturnsAuditModel = {
    val vrnOrAlternativeId: String = request.registrationWrapper.registration.customerIdentification match
      case EtmpCustomerIdentificationNew(idType, idValue) if idType != VRN => idValue
      case _ => request.vrnOrError.vrn

    ReturnsAuditModel(
      userId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      vrn = vrnOrAlternativeId,
      userAnswers = userAnswers.toUserAnswersForAudit,
      submissionResult = submissionResult
    )
  }
}
