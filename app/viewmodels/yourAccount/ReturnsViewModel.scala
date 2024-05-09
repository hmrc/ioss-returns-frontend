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

package viewmodels.yourAccount

import controllers.CheckCorrectionsTimeLimit.isOlderThanThreeYears
import models.{StandardPeriod, SubmissionStatus}
import models.SubmissionStatus.{Due, Next, Overdue}
import pages.{EmptyWaypoints, Waypoints}
import play.api.i18n.Messages
import viewmodels.{LinkModel, Paragraph, ParagraphSimple, ParagraphWithId}

import java.time.Clock
import java.time.format.DateTimeFormatter

case class ReturnsViewModel(
                             contents: Seq[Paragraph],
                             linkToStart: Option[LinkModel] = None
                           )

object ReturnsViewModel {

  def apply(returns: Seq[Return], clock: Clock)(implicit messages: Messages): ReturnsViewModel = {
    val inProgress = returns.find(_.inProgress)
    val returnDue = returns.find(_.submissionStatus == Due)
    val overdueReturns = returns.filter(_.submissionStatus == Overdue)
    val nextReturn = returns.find(_.submissionStatus == Next)
    val outstandingReturnsOlderThanThreeYears = getOutstandingReturnsOlderThanThreeYears(returns, clock)

    nextReturn.map(
      nextReturn =>
        ReturnsViewModel(
          contents = Seq(nextReturnParagraph(nextReturn.period))
        )
    ).getOrElse(
      dueReturnsModel(outstandingReturnsOlderThanThreeYears, overdueReturns, inProgress, returnDue)
    )
  }

  private def getOutstandingReturnsOlderThanThreeYears(returns: Seq[Return], clock: Clock): Seq[Return] = {
    returns.filter { currentReturn =>
      Seq(SubmissionStatus.Due, SubmissionStatus.Overdue, SubmissionStatus.Next).contains(currentReturn.submissionStatus) &&
        isOlderThanThreeYears(currentReturn.dueDate, clock)
    }
  }

  private def startDueReturnLink(waypoints: Waypoints, period: StandardPeriod)(implicit messages: Messages) = {
    LinkModel(
      linkText = messages("yourAccount.yourReturns.dueReturn.startReturn"),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(waypoints, period).url
    )
  }

  private def startOverdueReturnLink(waypoints: Waypoints, period: StandardPeriod)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("yourAccount.yourReturns.startReturn", period.displayShortText),
      id = "start-your-return",
      url = controllers.routes.StartReturnController.onPageLoad(waypoints, period).url
    )

  private def continueDueReturnLink(period: StandardPeriod)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("yourAccount.yourReturns.dueReturn.continueReturn"),
      id = "continue-your-return",
      url = controllers.routes.ContinueReturnController.onPageLoad(period).url
    )

  private def continueOverdueReturnLink(period: StandardPeriod)(implicit messages: Messages) =
    LinkModel(
      linkText = messages("yourAccount.yourReturns.continueReturn", period.displayShortText),
      id = "continue-your-return",
      url = controllers.routes.ContinueReturnController.onPageLoad(period).url
    )

  private def returnDueParagraph(period: StandardPeriod)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnDue", period.displayShortText, period.paymentDeadlineDisplay))

  private def returnOverdueInProgressParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnOverdue.inProgress"))


  private def returnDueInProgressParagraph(period: StandardPeriod)(implicit messages: Messages) =
    ParagraphSimple(
      s"""${messages("yourAccount.yourReturns.inProgress", period.displayText)}
         |<br>${messages("yourAccount.yourReturns.inProgress.due", period.paymentDeadlineDisplay)}
         |<br>""".stripMargin)

  private def returnOverdueSingularParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue.singular"))

  private def returnOverdueParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue"))

  private def returnOverdueInProgressAdditionalParagraph()(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnOverdue.additional.inProgress"))


  private def returnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue.multiple", numberOfOverdueReturns))

  private def onlyReturnsOverdueParagraph(numberOfOverdueReturns: Int)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.onlyReturnsOverdue", numberOfOverdueReturns))

  private def returnsOverdueMoreThanThreeYearsParagraph(period: StandardPeriod)(implicit messages: Messages) =
    ParagraphSimple(messages("yourAccount.yourReturns.returnsOverdue.moreThanThreeYears", period.displayShortText))

  private def nextReturnParagraph(nextReturn: StandardPeriod)(implicit messages: Messages) =
    ParagraphWithId(messages("yourAccount.nextPeriod", nextReturn.displayShortText, nextReturn.lastDay.plusDays(1)
      .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))),
      "next-period"
    )

  private def dueReturnsModel(outstandingReturnsOlderThanThreeYears: Seq[Return], overdueReturns: Seq[Return], currentReturn: Option[Return], dueReturn: Option[Return])(implicit messages: Messages): ReturnsViewModel = {
    val waypoints = EmptyWaypoints

    val returnsViewModel = (overdueReturns.size, currentReturn, dueReturn) match {
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

      case (0, Some(_), Some(dueReturn)) =>
        ReturnsViewModel(
          contents = Seq(returnDueInProgressParagraph(dueReturn.period)),
          linkToStart = Some(continueDueReturnLink(dueReturn.period))
        )

      case (1, None, _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnOverdueSingularParagraph(), returnDueParagraph(dueReturn.period))).getOrElse(Seq(returnOverdueParagraph()))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(waypoints, overdueReturns.head.period))
        )

      case (1, Some(inProgress), _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period), returnOverdueInProgressAdditionalParagraph()))
          .getOrElse(Seq(returnOverdueInProgressParagraph()))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(continueOverdueReturnLink(inProgress.period))
        )

      case (x, None, _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnsOverdueParagraph(x), returnDueParagraph(dueReturn.period)))
          .getOrElse(Seq(onlyReturnsOverdueParagraph(x)))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(startOverdueReturnLink(waypoints, overdueReturns.minBy(_.period.lastDay.toEpochDay).period))
        )

      case (x, Some(inProgress), _) =>
        val contents = dueReturn.map(dueReturn =>
          Seq(returnDueParagraph(dueReturn.period), returnsOverdueParagraph(x)))
          .getOrElse(Seq(onlyReturnsOverdueParagraph(x)))
        ReturnsViewModel(
          contents = contents,
          linkToStart = Some(continueOverdueReturnLink(inProgress.period))
        )

      case _ =>
        throw new RuntimeException(s"Unexpected combination overdueReturns.size:${overdueReturns.size}, currentReturn:$currentReturn, dueReturn:$dueReturn}")
    }

    val outstandingReturnsContent =
      outstandingReturnsOlderThanThreeYears.map(outstandingReturn => returnsOverdueMoreThanThreeYearsParagraph(outstandingReturn.period))

    returnsViewModel.copy(
      contents = outstandingReturnsContent ++ returnsViewModel.contents
    )
  }
}
