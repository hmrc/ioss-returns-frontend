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

package controllers.fileUpload

import controllers.actions.*
import forms.DataErrorFormProvider
import models.upscan.*
import pages.Waypoints
import pages.fileUpload.{CsvValidationErrorsPage, DataErrorPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.fileUpload.DataErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataErrorController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         cc: AuthenticatedControllerComponents,
                                         formProvider: DataErrorFormProvider,
                                         view: DataErrorView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  protected val controllerComponents: MessagesControllerComponents = cc

  val form: Form[Boolean] = formProvider()

  def onPageLoad(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData() {
    implicit request =>

      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      val errors: Seq[CsvError] = request.userAnswers.get(CsvValidationErrorsPage).getOrElse(Nil)
      val (paragraphs, bullets) = errorMessages(errors)

      val preparedForm = request.userAnswers.get(DataErrorPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, waypoints, period, isIntermediary, companyName, errors, paragraphs, bullets))
  }

  def onSubmit(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      
      val period = request.userAnswers.period
      val isIntermediary = request.isIntermediary
      val companyName = request.companyName
      val errors: Seq[CsvError] = request.userAnswers.get(CsvValidationErrorsPage).getOrElse(Nil)
      val (paragraphs, bullets) = errorMessages(errors)
      
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, waypoints, period, isIntermediary, companyName, errors, paragraphs, bullets))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(DataErrorPage, value))
            _              <- cc.sessionRepository.set(updatedAnswers)
          } yield Redirect(DataErrorPage.navigate(waypoints, request.userAnswers, updatedAnswers).route)
      )
  }

  private def errorMessages(errors: Seq[CsvError])(implicit messages: Messages): (Seq[String], Seq[String]) = {

    val errorType = errors.map(_.getClass.getSimpleName).distinct
    val cells = errors.map(_.cellRef).distinct.sorted.mkString(", ")

    if (errorType.size >= 2) {
      (
        Seq(
          messages("dataError.errorMessage.genericError.p1"),
          messages("dataError.errorMessage.genericErrors.p2"),
          messages("dataError.errorMessage.genericErrors.p3")
        ),
        Seq(
          messages("dataError.errorMessage.genericErrors.bullet1"),
          messages("dataError.errorMessage.genericErrors.bullet2"),
          messages("dataError.errorMessage.genericErrors.bullet3"),
          messages("dataError.errorMessage.genericErrors.bullet4")
        )
      )
    } else {
      errors.headOption match {

        case Some(_: CsvError.InvalidCountry) =>
          (
            Seq(
              messages("dataError.errorMessage.incorrectCountry.p1", cells),
              messages("dataError.errorMessage.incorrectCountry.p2")
            ),
            Nil
          )

        case Some(_: CsvError.InvalidCharacter) =>
          (Seq(messages("dataError.errorMessage.invalidCharacter.p1", cells)), Nil)

        case Some(_: CsvError.InvalidNumberFormat) =>
          (Seq(messages("dataError.errorMessage.invalidNumber.p1", cells)), Nil)

        case Some(_: CsvError.NegativeNumber) =>
          (Seq(messages("dataError.errorMessage.negativeNumber.p1", cells)), Nil)

        case Some(_: CsvError.BlankCell) =>
          (Seq(messages("dataError.errorMessage.blankCell.p1", cells)), Nil)

        case Some(_: CsvError.VatRateNotAllowed) =>
          val countries =
            errors.collect { case e: CsvError.VatRateNotAllowed => e.country }
              .distinct.sorted.mkString(", ")

          (Seq(messages("dataError.errorMessage.incorrectVatRate.p1", countries)), Nil)

        case _ =>
          (Seq(messages("dataError.errorMessage.genericError.p1")), Nil)
      }
    }
  }

}
