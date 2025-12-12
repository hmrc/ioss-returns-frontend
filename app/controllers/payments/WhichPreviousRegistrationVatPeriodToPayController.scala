/*
 * Copyright 2025 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.actions._
import forms.payments.WhichPreviousRegistrationVatPeriodToPayFormProvider
import models.Period
import models.payments.{Payment, PaymentStatus, PrepareData}
import pages.{JourneyRecoveryPage, Waypoints, YourAccountPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SelectedIossNumberRepository
import services.PaymentsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.payments.{NoPaymentsView, WhichPreviousRegistrationVatPeriodToPayView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichPreviousRegistrationVatPeriodToPayController @Inject()(
                                                                   override val messagesApi: MessagesApi,
                                                                   cc: AuthenticatedControllerComponents,
                                                                   selectedIossNumberRepository: SelectedIossNumberRepository,
                                                                   frontendAppConfig: FrontendAppConfig,
                                                                   paymentsService: PaymentsService,
                                                                   formProvider: WhichPreviousRegistrationVatPeriodToPayFormProvider,
                                                                   view: WhichPreviousRegistrationVatPeriodToPayView,
                                                                   viewNoPayment: NoPaymentsView
                                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  val form: Form[Period] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced.async {
    implicit request =>

      selectedIossNumberRepository.get(request.userId).flatMap { maybeSelectedIossNumber =>

        val iossNumber: String = maybeSelectedIossNumber.map(_.iossNumber).getOrElse(request.iossNumber)

        val prepareFinancialData: Future[PrepareData] = paymentsService.prepareFinancialDataWithIossNumber(iossNumber)
        prepareFinancialData.flatMap { pfd =>

          val payments = (pfd.duePayments ++ pfd.overduePayments).sortBy(p => (p.period.year, p.period.month)).reverse
          val paymentError = payments.exists(_.paymentStatus == PaymentStatus.Unknown)

          payments match {
            case payment :: Nil => makePayment(iossNumber, payment)
            case Nil => Future.successful(Ok(viewNoPayment(YourAccountPage.route(waypoints).url)))
            case _ => Future.successful(Ok(view(form, waypoints, payments, paymentError = paymentError)))
          }
        }
      }
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced.async {
    implicit request =>

      selectedIossNumberRepository.get(request.userId).flatMap { maybeSelectedIossNumber =>

        val iossNumber: String = maybeSelectedIossNumber.map(_.iossNumber).getOrElse(request.iossNumber)

        val prepareFinancialData: Future[PrepareData] = paymentsService.prepareFinancialDataWithIossNumber(iossNumber)
        prepareFinancialData.flatMap { pfd =>

          val payments = (pfd.duePayments ++ pfd.overduePayments).sortBy(p => (p.period.year, p.period.month)).reverse
          val paymentError = payments.exists(_.paymentStatus == PaymentStatus.Unknown)

          if (payments.size == 1) {
            makePayment(iossNumber, payments.head)
          } else {
            form.bindFromRequest().fold(
              formWithErrors => BadRequest(view(formWithErrors, waypoints, payments, paymentError)).toFuture,
              value =>
                getChosenPayment(payments, value)
                  .map(p => makePayment(iossNumber, p)).getOrElse(
                    Redirect(JourneyRecoveryPage.route(waypoints).url).toFuture
                  )
            )
          }
        }
      }
  }

  private def makePayment(iossNumber: String, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    paymentsService.makePayment(iossNumber, payment.period, payment.amountOwed).map {
      case Right(value) =>
        Redirect(value.nextUrl)
      case _ => Redirect(s"${frontendAppConfig.paymentsBaseUrl}/pay/service-unavailable")
    }
  }

  private def getChosenPayment(allPayments: Seq[Payment], period: Period): Option[Payment] = {
    allPayments
      .find(_.period == period)
  }
}
