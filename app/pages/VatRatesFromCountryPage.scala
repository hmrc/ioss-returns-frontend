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

package pages

import controllers.routes
import models.{Index, UserAnswers, VatRateFromCountry}
import play.api.libs.json.JsPath
import play.api.mvc.Call

case class VatRatesFromCountryPage(index: Index) extends QuestionPage[List[VatRateFromCountry]] {

  override def path: JsPath = JsPath \ PageConstants.sales \ index.position \ toString

  override def toString: String = "vatRatesFromCountry"

  override def route(waypoints: Waypoints): Call = routes.VatRatesFromCountryController.onPageLoad(waypoints, index)

  override protected def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page =
    SalesToCountryPage(index, Index(0))
}
