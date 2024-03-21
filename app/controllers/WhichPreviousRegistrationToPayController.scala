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

package controllers

import controllers.actions._
import forms.WhichPreviousRegistrationToPayFormProvider
import models.Period
import models.payments.PrepareData
import pages.{JourneyRecoveryPage, Waypoints, WhichPreviousRegistrationToPayPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PreviousRegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.WhichPreviousRegistrationToPayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichPreviousRegistrationToPayController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          cc: AuthenticatedControllerComponents,
                                                          formProvider: WhichPreviousRegistrationToPayFormProvider,
                                                          previousRegistrationService: PreviousRegistrationService,
                                                          view: WhichPreviousRegistrationToPayView
                                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      previousRegistrationService.getPreviousRegistrationPrepareFinancialData().flatMap { preparedDataList =>

        Ok(view(form, waypoints, preparedDataList)).toFuture
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      previousRegistrationService.getPreviousRegistrationPrepareFinancialData().flatMap { preparedDataList =>
//        preparedDataList.map { prepareData =>
//          val redirectLink = if (prepareData.overduePayments.size == 1) {
//            controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(waypoints).url
//          } else {
//            controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(waypoints).url
//          }

          form.bindFromRequest().fold(
            formWithErrors =>
              BadRequest(view(formWithErrors, waypoints, preparedDataList)).toFuture,

            value =>

              getSelectedItem(preparedDataList, value)
                .map(_ => determineRedirect(waypoints, preparedDataList)).getOrElse(
                  Redirect(JourneyRecoveryPage.route(waypoints).url)
                )
          )
//        }
      }
  }

  private def getSelectedItem(preparedData: List[PrepareData], iossNumber: String): Option[PrepareData] = {
    preparedData.find(_.iossNumber == iossNumber)
  }

  private def determineRedirect(waypoints: Waypoints, preparedDataList: List[PrepareData]): List[Result] = {
    preparedDataList.map { prepareData =>
      if (prepareData.overduePayments.size == 1) {
        Redirect(controllers.payments.routes.PaymentController.makePaymentForIossNumber(waypoints, prepareData.overduePayments.head.period, prepareData.iossNumber).url)
      } else {
        Redirect(controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(waypoints).url)
      }
    }
  }
}
