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

package controllers.intermediary

import controllers.actions.AuthenticatedControllerComponents
import config.FrontendAppConfig
import forms.IossOrIntermediaryFormProvider
import pages.EmptyWaypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IossOrIntermediaryView
import utils.EnrolmentIdentifiers.*
import utils.FutureSyntax.FutureOps

import javax.inject.Inject

class IossOrIntermediaryController @Inject()(
                                              cc: AuthenticatedControllerComponents,
                                              formProvider: IossOrIntermediaryFormProvider,
                                              view: IossOrIntermediaryView,
                                              frontendAppConfig: FrontendAppConfig
                                            ) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc
  private val form = formProvider()
  
  def onPageLoad(): Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced() {
    implicit request =>

      val allEnrolments: Seq[String] = findAllEnrolments(request.enrolments)
      val totalNumberOfEnrolments: Int = allEnrolments.size

      Ok(view(form, allEnrolments, totalNumberOfEnrolments))
  }

  def onSubmit(): Action[AnyContent] = cc.authAndGetRegistrationAndCheckBounced().async {
    implicit request =>

      val intermediaryNumber = request.intermediaryNumber.getOrElse("")
      val allEnrolments: Seq[String] = findAllEnrolments(request.enrolments)
      val totalNumberOfEnrolments: Int = allEnrolments.size

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors, allEnrolments, totalNumberOfEnrolments)).toFuture,

        value =>

          if (value == intermediaryNumber) {
            Redirect(frontendAppConfig.intermediaryDashboardUrl).toFuture
          } else {
            Redirect(controllers.routes.YourAccountController.onPageLoad(waypoints = EmptyWaypoints)).toFuture
          }
      )
  }

  private def findAllEnrolments(enrolments: Enrolments): Seq[String] = {

    val numberOfIossEnrolments: Seq[String] = findIossFromEnrolments(enrolments)
    val numberOfIntermediaryEnrolments: Seq[String] = findIntermediaryFromEnrolments(enrolments)

    numberOfIossEnrolments ++ numberOfIntermediaryEnrolments
  }
}
