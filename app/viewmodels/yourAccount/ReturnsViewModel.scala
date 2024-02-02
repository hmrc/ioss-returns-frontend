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

package viewmodels.yourAccount

import models.Period
import models.SubmissionStatus.{Due, Next, Overdue}
import pages.{EmptyWaypoints, Waypoints}
import play.api.i18n.Messages
import viewmodels.{LinkModel, Paragraph, ParagraphSimple, ParagraphWithId}

import java.time.format.DateTimeFormatter

case class ReturnsViewModel(
                           contents: Seq[Paragraph],
                           linkToStart: Option[LinkModel] = None
                           )

object ReturnsViewModel {

  def apply(returns: Seq[Return])(implicit messages: Messages): ReturnsViewModel = {
    val inProgress = returns.find(_.inProgress)
    val returnDue = returns.find(_.submissionStatus == Due)
    val overdueReturns = returns.filter(_.submissionStatus == Overdue)
    val nextReturn = returns.find(_.submissionStatus == Next)

    nextReturn.map(
      nextReturn =>
        ReturnsViewModel(
          contents = Seq(nextReturnParagraph(nextReturn.period))
        )
    ).getOrElse(
      dueReturnsModel(overdueReturns, inProgress, returnDue)
    )
  }

  private def startDueReturnLink(waypoints: Waypoints, period: Period)(implicit messages: Messages) = {
    LinkModel(
      linkText = messages("yourAccount.yourReturns.dueReturn.startReturn"),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(waypoints, period).url
    )
  }

  private def startOverdueReturnLink(waypoints: Waypoints, period: Period)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("yourAccount.yourReturns.dueReturn.startReturn"),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(waypoints, period).url
    )

  private def returnDueParagraph(period: Period)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnDue", period.displayShortText, period.paymentDeadlineDisplay))

  private def returnOverdueSingularParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue.singular"))

  private def returnOverdueParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue"))

  private def returnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue.multiple", numberOfOverdueReturns))
  private def onlyReturnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.onlyReturnsOverdue", numberOfOverdueReturns))
  private def nextReturnParagraph(nextReturn: Period)(implicit messages: Messages) =
    ParagraphWithId(messages("yourAccount.nextPeriod", nextReturn.displayShortText, nextReturn.lastDay.plusDays(1)
      .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
      "next-period"
    )

  private def dueReturnsModel(overdueReturns: Seq[Return], currentReturn: Option[Return], dueReturn: Option[Return])(implicit messages: Messages) = {
    val waypoints = EmptyWaypoints

    (overdueReturns.size, currentReturn, dueReturn) match {
      case (0, None, None) =>
        ReturnsViewModel(
          contents = Seq.empty,
          linkToStart = None
        )

      case (0, None, Some(dueReturn)) =>
        ReturnsViewModel(
          contents = Seq(returnDueParagraph(dueReturn.period)),
          linkToStart = Some(startDueReturnLink(waypoints, dueReturn.period))
        )

      case (1, None, _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnOverdueSingularParagraph(), returnDueParagraph(dueReturn.period))).getOrElse(Seq(returnOverdueParagraph()))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(waypoints, overdueReturns.head.period))
        )

      case (x, None, _) =>
        val contents = dueReturn.map(dueReturn =>
            Seq(returnsOverdueParagraph(x), returnDueParagraph(dueReturn.period)))
          .getOrElse(Seq(onlyReturnsOverdueParagraph(x)))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(waypoints, overdueReturns.minBy(_.period.lastDay.toEpochDay).period))
        )

      case _ =>
        throw new RuntimeException(s"Unexpected combination overdueReturns.size:${overdueReturns.size}, currentReturn:$currentReturn, dueReturn:$dueReturn}")

    }
  }
}
