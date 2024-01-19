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

package controllers.submissionResults

import controllers.actions._
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TotalAmountVatDueGBPQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CurrencyFormatter
import utils.Formatters.generateVatReturnReference
import views.html.submissionResults.SuccessfullySubmittedView

import javax.inject.Inject

class SuccessfullySubmittedController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 view: SuccessfullySubmittedView
                                               ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>
      val returnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)
      val hasSoldGoods = request.userAnswers.get(SoldGoodsPage)
        .getOrElse(throw new RuntimeException("SoldGoodsPage has not been set in answers"))

      lazy val correctPreviousReturnsBack = request.userAnswers.get(CorrectPreviousReturnPage(0))
        .getOrElse(throw new RuntimeException("CorrectPreviousReturnPage has not been set in answers"))

      val nilReturn = !hasSoldGoods && !correctPreviousReturnsBack

      val totalOwed = request.userAnswers.get(TotalAmountVatDueGBPQuery)
        .getOrElse(throw new RuntimeException("TotalAmountVatDueGBPQuery has not been set in answers"))

      Ok(view(
        returnReference,
        nilReturn = nilReturn,
        period = request.userAnswers.period,
        owedAmount = CurrencyFormatter.currencyFormatWithAccuracy(totalOwed))
      )
  }
}
