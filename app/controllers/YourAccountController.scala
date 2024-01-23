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
import connectors.ReturnStatusConnector
import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.{NoLongerSupplies, Reversal, TransferringMSID, VoluntarilyLeaves}
import models.requests.RegistrationRequest
import pages.Waypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PaymentsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.PaymentsViewModel
import viewmodels.yourAccount.{Return, ReturnsViewModel}
import views.html.YourAccountView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourAccountController @Inject()(
                                       cc: AuthenticatedControllerComponents,
                                       paymentsService: PaymentsService,
                                       returnStatusConnector: ReturnStatusConnector,
                                       view: YourAccountView,
                                       clock: Clock,
                                       appConfig: FrontendAppConfig
                                     )(implicit ec: ExecutionContext)

  extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetRegistration.async {
    implicit request =>

      val maybeExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.lastOption

      val cancelYourRequestToLeaveUrl = maybeExclusion match {
        case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
          LocalDate.now(clock).isBefore(exclusion.effectiveDate) => Some(appConfig.cancelYourRequestToLeaveUrl)
        case _ => None
      }

      val leaveThisServiceUrl = if (maybeExclusion.isEmpty || maybeExclusion.exists(_.exclusionReason == Reversal)) {
        Some(appConfig.leaveThisServiceUrl)
      } else {
        None
      }

      paymentsService.prepareFinancialData().map(payments => {
        val paymentsViewModel = PaymentsViewModel(payments.duePayments, payments.overduePayments)
        Ok(view(
          request.registrationWrapper.vatInfo.getName,
          request.iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          leaveThisServiceUrl,
          cancelYourRequestToLeaveUrl,
          returnsViewModel = ???
        ))
      })
  }

  private def getCurrentReturnsAndFinancialData()(implicit request: RegistrationRequest[AnyContent]) = {
    for {
      currentReturns <- returnStatusConnector.getCurrentReturns(request.iossNumber)
      currentPayments <- paymentsService.prepareFinancialData()
    } yield {
      (currentReturns, currentPayments)
    }
  }

  private def preparedViewWithFinancialData(
                                             returnsViewModel: Seq[Return]
                                           )(implicit  request: RegistrationRequest[AnyContent]): Future[Result] = {

    val lastExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.maxByOption(_.effectiveDate)

    val cancelYourRequestToLeaveUrl = lastExclusion match {
      case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
        LocalDate.now(clock).isBefore(exclusion.effectiveDate) => Some(appConfig.cancelYourRequestToLeaveUrl)
      case _ => None
    }

    val leaveThisServiceUrl = if (lastExclusion.isEmpty || lastExclusion.exists(_.exclusionReason == Reversal)) {
      Some(appConfig.leaveThisServiceUrl)
    } else {
      None
    }

    paymentsService.prepareFinancialData().map(payments => {
      val paymentsViewModel = PaymentsViewModel(payments.duePayments, payments.overduePayments)
        Ok(view(
          request.registrationWrapper.vatInfo.getName,
          request.iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          leaveThisServiceUrl,
          cancelYourRequestToLeaveUrl,
          ReturnsViewModel(
            returnsViewModel.map(currentReturn =>
            currentReturn
            )
          )
        ))
      })
  }

  private def preparedViewWithNoFinancialData(
                                             returnsViewModel: Seq[Return]
                                           )(implicit  request: RegistrationRequest[AnyContent]): Future[Result] = {

    val lastExclusion: Option[EtmpExclusion] = request.registrationWrapper.registration.exclusions.maxByOption(_.effectiveDate)

    val cancelYourRequestToLeaveUrl = lastExclusion match {
      case Some(exclusion) if Seq(NoLongerSupplies, VoluntarilyLeaves, TransferringMSID).contains(exclusion.exclusionReason) &&
        LocalDate.now(clock).isBefore(exclusion.effectiveDate) => Some(appConfig.cancelYourRequestToLeaveUrl)
      case _ => None
    }

    val leaveThisServiceUrl = if (lastExclusion.isEmpty || lastExclusion.exists(_.exclusionReason == Reversal)) {
      Some(appConfig.leaveThisServiceUrl)
    } else {
      None
    }

    Ok(view(
      request.registrationWrapper.vatInfo.getName,
      request.iossNumber,
      PaymentsViewModel(Seq.empty, Seq.empty),
      appConfig.amendRegistrationUrl,
      leaveThisServiceUrl,
      cancelYourRequestToLeaveUrl,
      ReturnsViewModel(
        returnsViewModel.map(currentReturn =>
          currentReturn
        )
      )
    )).toFuture
  }


}
