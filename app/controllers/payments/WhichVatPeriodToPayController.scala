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

package controllers.payments

import config.Service
import controllers.actions._
import forms.payments.WhichVatPeriodToPayFormProvider
import models.Period
import models.payments.{Payment, PaymentStatus, PrepareData}
import pages.{JourneyRecoveryPage, Waypoints, YourAccountPage}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Logging}
import services.PaymentsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.payments.{NoPaymentsView, WhichVatPeriodToPayView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichVatPeriodToPayController @Inject()(
                                               cc: AuthenticatedControllerComponents,
                                               config: Configuration,
                                               paymentsService: PaymentsService,
                                               formProvider: WhichVatPeriodToPayFormProvider,
                                               view: WhichVatPeriodToPayView,
                                               viewNoPayment: NoPaymentsView
                                             )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  private val form = formProvider()
  protected val controllerComponents: MessagesControllerComponents = cc

  val paymentsBaseUrl = config.get[Service]("microservice.services.pay-api")

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request => {
      val prepareFinancialData: Future[PrepareData] = paymentsService.prepareFinancialData()
      prepareFinancialData.flatMap { pfd =>
        val payments = pfd.duePayments ++ pfd.overduePayments
        val paymentError = payments.exists(_.paymentStatus == PaymentStatus.Unknown)

        payments match {
          case payment :: Nil => makePayment(request.iossNumber, payment)
          case Nil => Future.successful(Ok(viewNoPayment(YourAccountPage.route(waypoints).url)))
          case _ => Future.successful(Ok(view(form, payments, paymentError = paymentError)))
        }
      }
    }
  }

  private def makePayment(iossNumber: String, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    paymentsService.makePayment(iossNumber, payment.period, payment).map {
      case Right(value) => Redirect(value.nextUrl)
      case _ => Redirect(s"$paymentsBaseUrl/pay/service-unavailable")
    }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData.async {
    implicit request => {
      val prepareFinancialData: Future[PrepareData] = paymentsService.prepareFinancialData()
      prepareFinancialData.flatMap { pfd =>
        val payments = pfd.duePayments ++ pfd.overduePayments
        val paymentError = payments.exists(_.paymentStatus == PaymentStatus.Unknown)
        if (payments.size == 1) {
          makePayment(request.iossNumber, payments.head)
        } else {
          form.bindFromRequest().fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, payments, paymentError))),
            value =>
              getChosenPayment(payments, value)
                .map(p => makePayment(request.iossNumber, p)).getOrElse(
                Future.successful(Redirect(JourneyRecoveryPage.route(waypoints).url))
              ))
        }
      }
    }
  }

  private def getChosenPayment(allPayments: Seq[Payment], period: Period): Option[Payment] = {
    allPayments
      .find(_.period == period)
  }
}
