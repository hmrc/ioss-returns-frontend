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

package controllers.payments

import config.Service
import controllers.actions._
import models.Period
import models.payments.{Payment, PaymentStatus}
import pages.Waypoints
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PaymentsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PaymentController @Inject()(
                                   cc: AuthenticatedControllerComponents,
                                   config: Configuration,
                                   paymentsService: PaymentsService,
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val paymentsBaseUrl: Service = config.get[Service]("microservice.services.pay-api")

  def makePayment(waypoints: Waypoints, period: Period, amount: Long, paymentStatus: PaymentStatus): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      val payment: Payment = Payment(
        period = period,
        amountOwed = amount,
        dateDue = period.paymentDeadline,
        paymentStatus = paymentStatus
      )
      paymentsService.makePayment(request.iossNumber, period, payment).map {
        case Right(value) => Redirect(value.nextUrl)
        case _ => Redirect(s"$paymentsBaseUrl/pay/service-unavailable")
      }
  }
}
