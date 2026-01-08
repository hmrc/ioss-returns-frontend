/*
 * Copyright 2026 HM Revenue & Customs
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
import connectors.{FinancialDataConnector, VatReturnConnector}
import controllers.actions._
import logging.Logging
import models.Period
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.Configuration
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{PaymentsService, PreviousRegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentController @Inject()(
                                   cc: AuthenticatedControllerComponents,
                                   config: Configuration,
                                   paymentsService: PaymentsService,
                                   financialDataConnector: FinancialDataConnector,
                                   vatReturnConnector: VatReturnConnector,
                                   previousRegistrationService: PreviousRegistrationService
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val paymentsBaseUrl: Service = config.get[Service]("microservice.services.pay-api")

  def makePayment(waypoints: Waypoints, period: Period): Action[AnyContent] = cc.authAndGetOptionalData().async { implicit request =>
    getAmountOwedAndRedirect(period, request.iossNumber)
  }

  def makePaymentForIossNumber(waypoints: Waypoints, period: Period, iossNumber: String): Action[AnyContent] = {
    cc.authAndGetOptionalData().async { implicit request =>
      previousRegistrationService.getPreviousRegistrations(request.isIntermediary).flatMap { previousRegistrations =>
        val validIossNumbers: Seq[String] = request.iossNumber :: previousRegistrations.map(_.iossNumber)
        if (validIossNumbers.contains(iossNumber)) {
          getAmountOwedAndRedirect(period, iossNumber)
        } else {
          Future.successful(Redirect(JourneyRecoveryPage.route(waypoints)))
        }
      }
    }
  }

  private def getAmountOwedAndRedirect(period: Period, iossNumber: String)(implicit hc: HeaderCarrier): Future[Result] = {
    financialDataConnector.getChargeForIossNumber(period, iossNumber).flatMap {
      case Right(Some(charge)) =>
        makePaymentAndRedirect(period, (charge.outstandingAmount * 100).toLong, iossNumber)
      case _ =>
        getAmountFromReturn(period, iossNumber).flatMap { outstandingAmount =>
          makePaymentAndRedirect(period, outstandingAmount, iossNumber)
        }
    }
  }

  private def getAmountFromReturn(period: Period, iossNumber: String)(implicit hc: HeaderCarrier): Future[Long] = {
    vatReturnConnector.getForIossNumber(period, iossNumber).map {
      case Right(vatReturn) => (vatReturn.totalVATAmountDueForAllMSGBP * 100).toLong
      case Left(error) =>
        val message = s"Failed to get payment and failed to get backup from view returns got ${error.body}"
        logger.error(message)
        throw new Exception(message)
    }
  }

  private def makePaymentAndRedirect(period: Period, amountInPence: Long, iossNumber: String)(implicit hc: HeaderCarrier): Future[Result] = {

    val amountOwed = BigDecimal(amountInPence) / 100

    paymentsService.makePayment(iossNumber, period, amountOwed).map {
      case Right(value) => Redirect(value.nextUrl)
      case _ => Redirect(s"$paymentsBaseUrl/pay/service-unavailable")
    }
  }
}
