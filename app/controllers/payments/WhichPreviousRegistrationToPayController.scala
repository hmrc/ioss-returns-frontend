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
import forms.payments.WhichPreviousRegistrationToPayFormProvider
import logging.Logging
import models.payments.{Payment, PrepareData}
import models.requests.RegistrationRequest
import pages.payments.WhichPreviousRegistrationVatPeriodToPayPage
import pages.{JourneyRecoveryPage, Waypoints, YourAccountPage}
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SelectedIossNumberRepository
import services.{PaymentsService, PreviousRegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.payments.SelectedIossNumber
import views.html.payments.{NoPaymentsView, WhichPreviousRegistrationToPayView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichPreviousRegistrationToPayController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          cc: AuthenticatedControllerComponents,
                                                          selectedIossNumberRepository: SelectedIossNumberRepository,
                                                          config: Configuration,
                                                          formProvider: WhichPreviousRegistrationToPayFormProvider,
                                                          previousRegistrationService: PreviousRegistrationService,
                                                          paymentsService: PaymentsService,
                                                          view: WhichPreviousRegistrationToPayView,
                                                          viewNoPayment: NoPaymentsView
                                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val paymentsBaseUrl: Service = config.get[Service]("microservice.services.pay-api")

  val form: Form[String] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      previousRegistrationService.getPreviousRegistrationPrepareFinancialData().flatMap { preparedDataList =>
        determineRedirectOnLoad(waypoints, preparedDataList)
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

  private def determineRedirectOnLoad(
                                       waypoints: Waypoints,
                                       prepareDataList: List[PrepareData]
                                     )(implicit request: RegistrationRequest[AnyContent]): Future[Result] = {
    prepareDataList match {
      case Nil =>
        val message = s"There was an issue retrieving prepared financial data"
        logger.error(message)
        throw new Exception(message)
      case prepareData :: Nil =>
        val iossNumber = prepareData.iossNumber
        val payments = prepareData.overduePayments ++ prepareData.duePayments
        payments match {
          case Nil => Ok(viewNoPayment(YourAccountPage.route(waypoints).url)).toFuture
          case payment :: Nil =>
            makePayment(iossNumber, payment)
          case _ =>
            selectedIossNumberRepository.set(SelectedIossNumber(request.userId, iossNumber)).map { _ =>
              Redirect(WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints))
            }
        }
      case _ =>
        Ok(view(form, waypoints, prepareDataList)).toFuture
    }
  }

  private def getSelectedItemAndDetermineRedirect(
                                                   waypoints: Waypoints,
                                                   preparedDataList: List[PrepareData],
                                                   iossNumber: String
                                                 )(implicit request: RegistrationRequest[AnyContent]): Future[Result] = {
    preparedDataList.find(_.iossNumber == iossNumber).map { prepareData =>
      val payments = prepareData.overduePayments ++ prepareData.duePayments
      if (payments.size == 1) {
        makePayment(iossNumber, payments.head)
      } else {
        selectedIossNumberRepository.set(SelectedIossNumber(request.userId, iossNumber)).map { _ =>
          Redirect(WhichPreviousRegistrationVatPeriodToPayPage.route(waypoints))
        }
      }
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
  }

  private def makePayment(iossNumber: String, payment: Payment)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    paymentsService.makePayment(iossNumber, payment.period, payment.amountOwed).map {
      case Right(value) =>
        Redirect(value.nextUrl)
      case _ => Redirect(s"$paymentsBaseUrl/pay/service-unavailable")
    }
  }
}
