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
import models.payments.PrepareData
import pages.{JourneyRecoveryPage, Waypoints}
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

        form.bindFromRequest().fold(
          formWithErrors =>
            BadRequest(view(formWithErrors, waypoints, preparedDataList)).toFuture,

          value =>
            getSelectedItemAndDetermineRedirect(waypoints, preparedDataList, value)
        )
      }
  }

  private def getSelectedItemAndDetermineRedirect(waypoints: Waypoints, preparedDataList: List[PrepareData], iossNumber: String): Future[Result] = {
    preparedDataList.find(_.iossNumber == iossNumber).map { prepareData =>
      if (prepareData.overduePayments.size == 1) {
        Redirect(controllers.payments.routes.PaymentController
          .makePaymentForIossNumber(waypoints, prepareData.overduePayments.head.period, prepareData.iossNumber).url
        ).toFuture
      } else {
        Redirect(controllers.payments.routes.WhichVatPeriodToPayController.onPageLoad(waypoints).url).toFuture
      }
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture)
  }
}
