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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.TotalAmountVatDueGBPQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Formatters.generateVatReturnReference
import views.html.submissionResults.SuccessfullySubmittedView

import javax.inject.Inject
import scala.math.BigDecimal.RoundingMode

class SuccessfullySubmittedController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       cc: AuthenticatedControllerComponents,
                                       view: SuccessfullySubmittedView
                                     ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>
      val returnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)

     {for {
       hasSoldGoods <- request.userAnswers.get(SoldGoodsPage)
       correctPreviousReturnsBack <- request.userAnswers.get(CorrectPreviousReturnPage)
       totalOwed <- request.userAnswers.get(TotalAmountVatDueGBPQuery)
       nilReturn = !hasSoldGoods && !correctPreviousReturnsBack
      } yield (
        Ok(view(returnReference, nilReturn, request.userAnswers.period, totalOwed.setScale(2, RoundingMode.HALF_EVEN).toString))
      )}.getOrElse{
       //throw new RuntimeException("SoldGoodsPage or CorrectPreviousReturnPage have not been set")

       Ok(view(returnReference, false, request.userAnswers.period, "ddsddddd"))
     }
  }
}
