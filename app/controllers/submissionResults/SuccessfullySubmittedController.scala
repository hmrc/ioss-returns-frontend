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
import controllers.actions.*
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.TotalAmountVatDueGBPQuery
import services.intermediary.{DashboardNavigationService, IntermediaryClientService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.EnrolmentIdentifiers.{findIntermediaryFromEnrolments, findIossFromEnrolments}
import utils.Formatters.generateVatReturnReference
import views.html.submissionResults.SuccessfullySubmittedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SuccessfullySubmittedController @Inject()(
                                                 override val messagesApi: MessagesApi,
                                                 cc: AuthenticatedControllerComponents,
                                                 frontendAppConfig: FrontendAppConfig,
                                                 intermediaryClientService: IntermediaryClientService,
                                                 view: SuccessfullySubmittedView,
                                                 dashboardNavigationService: DashboardNavigationService
                                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  def onPageLoad(iossNumber: String): Action[AnyContent] = cc.authAndRequireData(iossNumber).async {
    implicit request =>
      val userResearchUrl = frontendAppConfig.userResearchUrl2
      val isIntermediary = request.isIntermediary
      val intermediaryClientName = intermediaryClientService.getClientName(isIntermediary, request.intermediaryNumber, request.iossNumber)

      val returnReference = generateVatReturnReference(request.iossNumber, request.userAnswers.period)
      val hasSoldGoodsPage = request.userAnswers.get(SoldGoodsPage(request.iossNumber))
      val hasCorrectedPreviousReturn = request.userAnswers.get(CorrectPreviousReturnPage(request.iossNumber, 0))

      val nilReturn = (hasSoldGoodsPage, hasCorrectedPreviousReturn) match {
        case (Some(false), Some(false)) => true
        case (Some(false), None) => true
        case _ => false
      }

      val totalOwed = request.userAnswers.get(TotalAmountVatDueGBPQuery)
        .getOrElse(throw new RuntimeException("TotalAmountVatDueGBPQuery has not been set in answers"))

      val iossEnrolmentsExist: Boolean = findIossFromEnrolments(request.enrolments).nonEmpty
      val intermediaryEnrolmentsExist: Boolean = findIntermediaryFromEnrolments(request.enrolments).nonEmpty

      for {
        clientName <- intermediaryClientName
        appropriateDashboardUrl <- dashboardNavigationService.getAppropriateDashboardUrl(
          isIntermediary, intermediaryEnrolmentsExist, iossEnrolmentsExist
        )
        _ <- cc.sessionRepository.clear(request.userId, request.iossNumber)
      } yield {

        Ok(view(
          returnReference,
          nilReturn = nilReturn,
          iossNumber = request.iossNumber,
          period = request.userAnswers.period,
          owedAmount = totalOwed,
          userResearchUrl,
          isIntermediary = isIntermediary,
          clientName = clientName,
          appropriateDashboardUrl = appropriateDashboardUrl
        ))
      }
  }
}
