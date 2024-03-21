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

package controllers.corrections

import controllers.actions._
import forms.corrections.RemoveCountryCorrectionFormProvider
import models.Index
import pages.Waypoints
import pages.corrections.RemoveCountryCorrectionPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import queries.{AllCorrectionPeriodsQuery, CorrectionPeriodQuery, CorrectionToCountryQuery, DeriveNumberOfCorrectionPeriods, DeriveNumberOfCorrections}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FutureSyntax.FutureOps
import views.html.corrections.RemoveCountryCorrectionView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveCountryCorrectionController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: RemoveCountryCorrectionFormProvider,
                                         view: RemoveCountryCorrectionView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      val period = request.userAnswers.period

      val preparedForm = request.userAnswers.get(RemoveCountryCorrectionPage(periodIndex, countryIndex)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, period, periodIndex, countryIndex))
  }

  def onSubmit(waypoints: Waypoints, periodIndex: Index, countryIndex: Index): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>

      val period = request.userAnswers.period

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period, periodIndex, countryIndex))),

        value =>
          if (value) {
            Future.fromTry(request.userAnswers.remove(CorrectionToCountryQuery(periodIndex, countryIndex))).flatMap {
              updatedAnswers =>
                if (updatedAnswers.get(DeriveNumberOfCorrections(periodIndex)).getOrElse(0) == 0) {
                  Future.fromTry(updatedAnswers.remove(CorrectionPeriodQuery(periodIndex))).flatMap {
                    answersWithRemovedPeriod =>
                      if (answersWithRemovedPeriod.get(DeriveNumberOfCorrectionPeriods).getOrElse(0) == 0) {
                        for {
                          answersWithRemovedCorrections <- Future.fromTry(answersWithRemovedPeriod.remove(AllCorrectionPeriodsQuery))
                          _ <- cc.sessionRepository.set(answersWithRemovedCorrections)
                        } yield {
                          Redirect(RemoveCountryCorrectionPage(periodIndex, countryIndex)
                            .navigate(waypoints, request.userAnswers, answersWithRemovedCorrections).route)
                        }
                      } else {
                        for {
                          _ <- cc.sessionRepository.set(answersWithRemovedPeriod)
                        } yield {
                          Redirect(RemoveCountryCorrectionPage(periodIndex, countryIndex)
                          .navigate(waypoints, request.userAnswers, answersWithRemovedPeriod).route)
                        }
                      }
                  }
                } else {
                  for {
                    _ <- cc.sessionRepository.set(updatedAnswers)
                  } yield {
                    Redirect(RemoveCountryCorrectionPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, updatedAnswers).route)
                  }
                }
            }

          } else {
            Redirect(RemoveCountryCorrectionPage(periodIndex, countryIndex).navigate(waypoints, request.userAnswers, request.userAnswers).route).toFuture
          }
      )
  }
}
