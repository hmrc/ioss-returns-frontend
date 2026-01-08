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

package controllers.submissionResults

import config.FrontendAppConfig
import connectors.{IntermediaryRegistrationConnector, VatReturnConnector}
import controllers.actions.*
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TotalAmountVatDueGBPQuery
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Formatters.generateVatReturnReference
import views.html.submissionResults.SuccessfullySubmittedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SuccessfullySubmittedController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 vatReturnConnector: VatReturnConnector,
                                                 frontendAppConfig: FrontendAppConfig,
                                                 intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                                 view: SuccessfullySubmittedView
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad: Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      val userResearchUrl = frontendAppConfig.userResearchUrl2
      val isIntermediary = request.isIntermediary
      val intermediaryClientName =
        if (isIntermediary) {
          request.intermediaryNumber match {
            case Some(num) =>
              intermediaryRegistrationConnector.get(num).map { registration =>
                registration.etmpDisplayRegistration.clientDetails
                  .find(_.clientIossID == request.iossNumber)
                  .map(_.clientName)
                  .getOrElse("")
              }
            case None =>
              Future.failed(new RuntimeException("No intermediary number in request"))
          }
        } else {
          Future.successful("")
        }

      val returnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)
      val hasSoldGoodsPage = request.userAnswers.get(SoldGoodsPage)
      val hasCorrectedPreviousReturn = request.userAnswers.get(CorrectPreviousReturnPage(0))

      val nilReturn = (hasSoldGoodsPage, hasCorrectedPreviousReturn) match {
        case (Some(false), Some(false)) => true
        case (Some(false), None) => true
        case _ => false
      }

      val totalOwed = request.userAnswers.get(TotalAmountVatDueGBPQuery)
        .getOrElse(throw new RuntimeException("TotalAmountVatDueGBPQuery has not been set in answers"))

      for {
        clientName <- intermediaryClientName
        errorOrExternalUrl <- vatReturnConnector.getSavedExternalEntry()
        _ <- cc.sessionRepository.clear(request.userId)
      } yield {
        val maybeExternalUrl = errorOrExternalUrl.fold(
          _ => None,
          _.url
        )

        val intermediaryDashboardUrl = frontendAppConfig.intermediaryDashboardUrl

        Ok(view(
          returnReference,
          nilReturn = nilReturn,
          period = request.userAnswers.period,
          owedAmount = totalOwed,
          externalUrl = maybeExternalUrl,
          userResearchUrl,
          isIntermediary = isIntermediary,
          clientName = clientName,
          intermediaryDashboardUrl = intermediaryDashboardUrl
        ))
      }
  }
}
