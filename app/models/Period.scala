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
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.{LocalDate, Month, YearMonth}
import java.time.format.{DateTimeFormatter, TextStyle}
import java.util.Locale
import scala.util.Try
import scala.util.matching.Regex

final case class Period(year: Int, month: Month) {
  val firstDay: LocalDate = YearMonth.of(year, month).atDay(1)
  val lastDay: LocalDate = YearMonth.of(year, month).atEndOfMonth

  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
  private val lastYearMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d")

  val paymentDeadline: LocalDate =
    firstDay.plusMonths(2).minusDays(1)

  private val paymentDeadlineFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  val paymentDeadlineDisplay: String = paymentDeadline.format(paymentDeadlineFormatter)

  def toEtmpPeriodString: String = {
    val lastYearDigits = year.toString.substring(2)

    s"$lastYearDigits${toEtmpMonthString(month)}"
  }

  private def toEtmpMonthString(month: Month): String = {
    month match {
      case Month.JANUARY => "AA"
      case Month.FEBRUARY => "AB"
      case Month.MARCH => "AC"
      case Month.APRIL => "AD"
      case Month.MAY => "AE"
      case Month.JUNE => "AF"
      case Month.JULY => "AG"
      case Month.AUGUST => "AH"
      case Month.SEPTEMBER => "AI"
      case Month.OCTOBER => "AJ"
      case Month.NOVEMBER => "AK"
      case Month.DECEMBER => "AL"
    }
  }

  def displayText: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${year}"

  def displayYearMonth: String =
    s"${lastDay.format(lastYearMonthFormatter)}"

  def displayShortText: String =
    s"${lastDay.format(lastMonthYearFormatter)}"

  private val firstDayFormatter = DateTimeFormatter.ofPattern("d MMMM")
  private val lastDayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def displayLongText(implicit messages: Messages): String =
    s"${firstDay.format(firstDayFormatter)} ${messages("site.to")} ${lastDay.format(lastDayFormatter)}"

  def zeroPaddedMonth: String =
    "%02d".format(month.getValue)

  def displayMonth: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"

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

  def fromKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val month = key.drop(2)
    Period(s"20$yearLast2".toInt, fromEtmpMonthString(month))
  }

  val reads: Reads[Period] = {
    (
      (__ \ "year").read[Int] and
        (__ \ "month").read[String].map(m => Month.of(m.substring(1).toInt))
      )((year, month) => Period(year, month))
  }

  val writes: OWrites[Period] = {
    (
      (__ \ "year").write[Int] and
        (__ \ "month").write[String].contramap[Month](m => s"M${m.getValue}")
      )(unlift(Period.unapply))
  }

  implicit val format: OFormat[Period] = OFormat(reads, writes)

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

  def monthOptions(periods: Seq[Period]): Seq[RadioItem] = periods.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(value.displayMonth),
        value = Some(value.toString),
        id = Some(s"value_${value.displayMonth}")
      )
  }

  private def fromEtmpMonthString(keyMonth: String): Month = {
    keyMonth match {
      case "AA" => Month.JANUARY
      case "AB" => Month.FEBRUARY
      case "AC" => Month.MARCH
      case "AD" => Month.APRIL
      case "AE" => Month.MAY
      case "AF" => Month.JUNE
      case "AG" => Month.JULY
      case "AH" => Month.AUGUST
      case "AI" => Month.SEPTEMBER
      case "AJ" => Month.OCTOBER
      case "AK" => Month.NOVEMBER
      case "AL" => Month.DECEMBER
    }
  }
}