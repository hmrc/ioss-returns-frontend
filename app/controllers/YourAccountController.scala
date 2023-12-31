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

package controllers

import config.FrontendAppConfig
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.payments.Payment
import pages.Waypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PaymentsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.PaymentsViewModel
import views.html.YourAccountView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       paymentsService: PaymentsService,
                                       view: YourAccountView,
                                       appConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext)

  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      paymentsService.getUnpaidPayments(request.iossNumber).map(payments => {
        val paymentsViewModel = PaymentsViewModel(payments.filter(isDue), payments.filterNot(isDue))
        Ok(view(request.registrationWrapper.vatInfo.getName, request.iossNumber, paymentsViewModel, appConfig.amendRegistrationUrl))
      })
  }

  private def isDue(payment: Payment): Boolean = {
    payment.period.lastDay.isAfter(LocalDate.now().minusDays(1))
  }
}
