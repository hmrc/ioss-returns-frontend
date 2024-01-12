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

package pages.corrections

import controllers.actions.AuthenticatedControllerComponents
import models.corrections.PeriodWithCorrections
import models.{Index, Period, UserAnswers}
import pages.{CheckYourAnswersPage, Page, Waypoints}
import play.api.mvc.Call
import queries.{AllCorrectionPeriodsQuery, CorrectionPeriodQuery, DeriveNumberOfCorrectionPeriods}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

case class VatPeriodCorrectionsListPage(period: Period, addAnother: Boolean) extends Page {
  override def nextPageNormalMode(waypoints: Waypoints, answers: UserAnswers): Page = {
    if (addAnother) {
      answers.get(DeriveNumberOfCorrectionPeriods) match {
        case Some(size) => CorrectionReturnPeriodPage(Index(size))
        case None => CorrectionReturnPeriodPage(Index(0))
      }
    } else {
      CheckYourAnswersPage
    }
  }

  def cleanup(userAnswers: UserAnswers, cc: AuthenticatedControllerComponents)(implicit ec: ExecutionContext): Future[Try[UserAnswers]] = {
    val periodsWithCorrections: Seq[PeriodWithCorrections] = userAnswers.get(AllCorrectionPeriodsQuery).getOrElse(List.empty)
    val emptyPeriods = periodsWithCorrections.zipWithIndex.filter(_._1.correctionsToCountry.isEmpty)
    val updatedAnswers = emptyPeriods.foldRight(Try(userAnswers)) { (indexedPeriodWithCorrection, userAnswersTry) =>
      userAnswersTry.flatMap(userAnswersToUpdate =>
        userAnswersToUpdate.remove(CorrectionPeriodQuery(Index(indexedPeriodWithCorrection._2))))
    }
    val finalAnswers = updatedAnswers.flatMap(userAnswers =>
      if (userAnswers.get(AllCorrectionPeriodsQuery).getOrElse(List.empty).isEmpty) {
        userAnswers.remove(AllCorrectionPeriodsQuery)
      } else {
        Try(userAnswers)
      }
    )
    for {
      _ <- cc.sessionRepository.set(finalAnswers.getOrElse(userAnswers))
    } yield finalAnswers
  }

  override def route(waypoints: Waypoints): Call = {
    controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, period)
  }
}
