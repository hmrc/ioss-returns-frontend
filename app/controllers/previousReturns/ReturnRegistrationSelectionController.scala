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

package controllers.previousReturns

import connectors.RegistrationConnector
import controllers.actions._
import forms.ReturnRegistrationSelectionFormProvider
import logging.Logging
import models.Period
import pages.Waypoints
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.previousReturns.PreviousRegistration
import views.html.previousReturns.ReturnRegistrationSelectionView

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnRegistrationSelectionController @Inject()(
                                                       override val messagesApi: MessagesApi,
                                                       cc: AuthenticatedControllerComponents,
                                                       formProvider: ReturnRegistrationSelectionFormProvider,
                                                       view: ReturnRegistrationSelectionView,
                                                       registrationConnector: RegistrationConnector
                                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc



  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndGetOptionalData().async {
    implicit request =>
      getPreviousRegistrationsStub().map { previousRegistrations =>
        val form: Form[PreviousRegistration] = formProvider(previousRegistrations)

        Ok(view(
          waypoints = waypoints,
          form,
          previousRegistrations
        ))
      }
  }

  private def getPreviousRegistrations()(implicit hc: HeaderCarrier): Future[Seq[PreviousRegistration]] = {
    registrationConnector.getAccounts().map { accounts =>
      println("accounts: " + accounts)
      val accountDetails: Seq[(YearMonth, String)] = accounts
        .enrolments.map(e => e.activationDate -> e.identifiers.find(_.key == "IOSSNumber").map(_.value))
        .collect {
          case (Some(activationDate), Some(iossNumber)) => YearMonth.from(activationDate) -> iossNumber
        }.sortBy(_._1)

      accountDetails.zip(accountDetails.drop(1)).map { case ((activationDate, iossNumber), (nextActivationDate, _)) =>
        PreviousRegistration(
          startPeriod = Period(activationDate),
          endPeriod = Period(nextActivationDate.minusMonths(1)),
          iossNumber = iossNumber
        )
      }
    }
  }

  private def getPreviousRegistrationsStub()(implicit hc: HeaderCarrier): Future[Seq[PreviousRegistration]] = {
    Future.successful(
      Seq(
        PreviousRegistration(
          "IM900987654321",
          Period(YearMonth.of(2020, 1)),
          Period(YearMonth.of(2021, 2))
        ),
        PreviousRegistration(
          "IM900987654322",
          Period(YearMonth.of(2021, 3)),
          Period(YearMonth.of(2021, 10))
        )
      )
    )
  }
}