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
import models.requests.{CorrectionRequest, DataRequest}
import play.api.libs.json.{JsObject, JsValue, Json}

case class ReturnsAuditModel(
                              userId: String,
                              userAgent: String,
                              //vatReturnRequest
                              correctionRequest: Option[CorrectionRequest],
                              //reference,
                              //paymentReference,
                              result: SubmissionResult
                            ) extends JsonAuditModel {

  override val auditType: String = "ReturnSubmitted"
  override val transactionName: String = "return-submitted"

  private val correctionDetails: JsObject = {
    correctionRequest match {
      case Some(corrRequest) =>
        Json.obj(
          "correctionDetails" -> Json.obj(
            "vatRegistrationNumber" -> corrRequest.vrn,
            "period" -> Json.toJson(corrRequest.period),
            "corrections" -> Json.toJson(corrRequest.corrections)
          )
        )
      case _ => Json.obj()
    }
  }

  override val detail: JsValue = Json.obj(
    "credId" -> userId,
    "browserUserAgent" -> userAgent,
    "submissionResult" -> Json.toJson(result),
    "returnDetails" -> ???
  ) ++ correctionDetails

}

object ReturnsAuditModel {

  def build(
           correctionRequest: Option[CorrectionRequest],
           result: SubmissionResult,
           request: DataRequest[_]
           ): ReturnsAuditModel = {
    ReturnsAuditModel(
      userId = request.credentials.providerId,
      userAgent = request.headers.get("user-agent").getOrElse(""),
      correctionRequest = correctionRequest,
      result = result,
    )
  }
}
