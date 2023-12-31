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

package models

import play.api.i18n.Messages
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.{LocalDate, Month}
import java.time.format.{DateTimeFormatter, TextStyle}
import java.util.Locale
import scala.util.Try
import scala.util.matching.Regex

final case class Period(year: Int, month: Month) {
  val firstDay: LocalDate = LocalDate.of(year, month, 1)
  val lastDay: LocalDate = firstDay.plusMonths(3).minusDays(1)
  private val firstMonthFormatter = DateTimeFormatter.ofPattern("MMMM")
  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  def displayText: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${year}"

  def displayShortText(implicit messages: Messages): String =
    s"${firstDay.format(firstMonthFormatter)} ${messages("site.to")} ${lastDay.format(lastMonthYearFormatter)}"

  override def toString: String = s"$year-M${month.getValue}"
}

object Period {
  private val pattern: Regex = """(\d{4})-M(1[0-2]|[1-9])""".r.anchored

  def apply(yearString: String, monthString: String): Try[Period] =
    for {
      year <- Try(yearString.toInt)
      month <- Try(Month.of(monthString.toInt))
    } yield Period(year, month)

  def fromString(string: String): Option[Period] =
    string match {
      case pattern(yearString, monthString) =>
        Period(yearString, monthString).toOption
      case _ =>
        None
    }

  implicit val monthReads: Reads[Month] = {
    Reads.at[Int](__ \ "month")
      .map(Month.of)
  }

  implicit val monthWrites: Writes[Month] = {
    Writes.at[Int](__ \ "month")
      .contramap(_.getValue)
  }

  implicit val format: OFormat[Period] = Json.format[Period]

  implicit val pathBindable: PathBindable[Period] = new PathBindable[Period] {

    override def bind(key: String, value: String): Either[String, Period] =
      fromString(value) match {
        case Some(period) => Right(period)
        case None => Left("Invalid period")
      }

    override def unbind(key: String, value: Period): String =
      value.toString
  }

  implicit val queryBindable: QueryStringBindable[Period] = new QueryStringBindable[Period] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Period]] = {
      params.get(key).flatMap(_.headOption).map {
        periodString =>
          fromString(periodString) match {
            case Some(period) => Right(period)
            case _ => Left("Invalid period")
          }
      }
    }

    override def unbind(key: String, value: Period): String = {
      s"$key=${value.toString}"
    }
  }

  def options(periods: Seq[Period]): Seq[RadioItem] = periods.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(value.displayText),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
  }
}