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

package pages.fileUpload

import models.UserAnswers
import pages.corrections.CorrectPreviousReturnPage
import pages.{CheckYourAnswersPage, Page, QuestionPage, SoldGoodsPage, Waypoints}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case object FileUploadedPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "fileUploaded"

  override def route(waypoints: Waypoints): Call = controllers.fileUpload.routes.FileUploadedController.onPageLoad(waypoints)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    val status = answers.get(FileUploadStatusPage).map(_.toUpperCase)
    (status, answers.get(this)) match {
      case (Some("UPLOADED"), Some(true)) =>
        if (answers.isDefined(CorrectPreviousReturnPage(0))) {
          CheckYourAnswersPage
        } else {
          CorrectPreviousReturnPage(0)
        }
      case (Some("UPLOADED"), Some(false)) =>
        FileUploadPage

      case (Some("FAILED"), Some(true)) =>
        FileUploadPage

      case (Some("FAILED"), Some(false)) =>
        SoldGoodsPage

      case _ =>
        this
    }
  }
}
