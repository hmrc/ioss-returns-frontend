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

import controllers.actions._
import forms.payments.WhichPreviousRegistrationToPayFormProvider
import models.payments.PrepareData
import models.requests.RegistrationRequest
import pages.payments.WhichVatPeriodToPayPage
import pages.{JourneyRecoveryPage, Waypoints}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SelectedPrepareDataRepository
import services.PreviousRegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import viewmodels.payments.SelectedPrepareData
import views.html.payments.WhichPreviousRegistrationToPayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichPreviousRegistrationToPayController @Inject()(
                                                          override val messagesApi: MessagesApi,
                                                          cc: AuthenticatedControllerComponents,
                                                          selectedPrepareDataRepository: SelectedPrepareDataRepository,
                                                          formProvider: WhichPreviousRegistrationToPayFormProvider,
                                                          previousRegistrationService: PreviousRegistrationService,
                                                          view: WhichPreviousRegistrationToPayView
                                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

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
      case Nil => Redirect(JourneyRecoveryPage.route(waypoints)).toFuture // TODO -> Where to go when no prepare data
      case prepareData :: Nil =>
        val iossNumber = prepareData.iossNumber
        prepareData.overduePayments match {
          case overduePayment :: Nil =>
            val period = overduePayment.period
            Redirect(controllers.payments.routes.PaymentController.makePaymentForIossNumber(waypoints, period, iossNumber)).toFuture
          case Nil => Redirect(JourneyRecoveryPage.route(waypoints)).toFuture // TODO -> Where to go when no payments due
          case _ =>
            // TODO -> Create new endpoint specifically for prev reg ioss.
            //  Also make repo just for ioss String
            selectedPrepareDataRepository.set(SelectedPrepareData(request.userId, prepareData)).map { _ =>
              Redirect(WhichVatPeriodToPayPage.route(waypoints))
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
      if (prepareData.overduePayments.size == 1) {
        Redirect(controllers.payments.routes.PaymentController
          .makePaymentForIossNumber(waypoints, prepareData.overduePayments.head.period, prepareData.iossNumber)
        ).toFuture
      } else {
        selectedPrepareDataRepository.set(SelectedPrepareData(request.userId, prepareData)).map { _ =>
          Redirect(WhichVatPeriodToPayPage.route(waypoints))
        }
      }
    }.getOrElse(Redirect(JourneyRecoveryPage.route(waypoints)).toFuture)
  }
}
