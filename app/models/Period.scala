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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{LocalDate, Month, YearMonth}
import java.util.Locale
import scala.util.Try
import scala.util.matching.Regex

trait Period {
  val year: Int
  val month: Month
  val firstDay: LocalDate
  val lastDay: LocalDate
  val isPartial: Boolean

  private val lastDayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val lastMonthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  val paymentDeadline: LocalDate = LocalDate.of(year, month, 1).plusMonths(2).minusDays(1)

  private val paymentDeadlineFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

  val paymentDeadlineDisplay: String = paymentDeadline.format(paymentDeadlineFormatter)

  def displayText: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} $year"

  def displayMonth: String =
    s"${month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}"

  def getNext: Period = {
    if (this.month == Month.DECEMBER) {
      StandardPeriod(this.year + 1, Month.JANUARY)
    } else {
      StandardPeriod(this.year, this.month.plus(1))
    }
  }

  def isBefore(other: Period): Boolean = {
    val yearMonth: YearMonth = YearMonth.of(year, month)
    val yearMonthOther: YearMonth = YearMonth.of(other.year, other.month)

    yearMonth.isBefore(yearMonthOther)
  }

  def zeroPaddedMonth: String =
    "%02d".format(month.getValue)

  def displayShortText: String =
    s"${lastDay.format(lastMonthYearFormatter)}"

  def displayPartialPeriodLastDayText: String =
    s"${lastDay.format(lastDayFormatter)}"

  def displayPartialPeriodStartDayText: String =
    s"${firstDay.format(lastDayFormatter)}"

  override def toString: String = s"$year-M${month.getValue}"

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


}

final case class StandardPeriod(year: Int, month: Month) extends Period with Ordered[StandardPeriod] {

  private val yearMonth: YearMonth = YearMonth.of(year, month)
  override val firstDay: LocalDate = yearMonth.atDay(1)
  override val lastDay: LocalDate = yearMonth.atEndOfMonth
  override val isPartial: Boolean = false

  private val lastYearMonthFormatter = DateTimeFormatter.ofPattern("yyyy-MM-d")

  def displayYearMonth: String =
    s"${lastDay.format(lastYearMonthFormatter)}"

  def compare(other: StandardPeriod): Int = yearMonth.compareTo(other.yearMonth)

}

object StandardPeriod {

  val reads: Reads[StandardPeriod] = {
    (
      (__ \ "year").read[Int] and
        (__ \ "month").read[String].map(m => Month.of(m.substring(1).toInt))
      )((year, month) => StandardPeriod(year, month))
  }

  val writes: OWrites[StandardPeriod] = {
    (
      (__ \ "year").write[Int] and
        (__ \ "month").write[String].contramap[Month](m => s"M${m.getValue}")
      )(unlift(StandardPeriod.unapply))
  }

  implicit val format: Format[StandardPeriod] = Format(reads, writes)

  def apply(yearMonth: YearMonth): Period = StandardPeriod(yearMonth.getYear, yearMonth.getMonth)


  def apply(yearString: String, monthString: String): Try[StandardPeriod] =
    for {
      year <- Try(yearString.toInt)
      month <- Try(Month.of(monthString.toInt))
    } yield StandardPeriod(year, month)

  def options(periods: Seq[StandardPeriod]): Seq[RadioItem] = periods.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        content = Text(value.displayText),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
  }


  def fromString(string: String): Option[StandardPeriod] = {
    Period.fromString(string).map(fromPeriod)
  }

  def fromPeriod(period: Period): StandardPeriod = {
    StandardPeriod(period.year, period.month)
  }

}

object Period {
  private val pattern: Regex = """(\d{4})-M(1[0-2]|[1-9])""".r.anchored

  def fromString(string: String): Option[Period] =
    string match {
      case pattern(yearString, monthString) =>
        StandardPeriod(yearString, monthString).toOption
      case _ =>
        None
    }

  def fromKey(key: String): Period = {
    val yearLast2 = key.take(2)
    val month = key.drop(2)
    StandardPeriod(s"20$yearLast2".toInt, fromEtmpMonthString(month))
  }

  val reads: Reads[Period] = {
    PartialReturnPeriod.format.widen[Period] orElse
      StandardPeriod.format.widen[Period]
  }

  val writes: Writes[Period] = {
    case s: StandardPeriod => Json.toJson(s)(StandardPeriod.format)
    case p: PartialReturnPeriod => Json.toJson(p)(PartialReturnPeriod.format)
  }

  implicit val format: Format[Period] = Format(reads, writes)

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

  def monthOptions(periods: Seq[Period]): Seq[RadioItem] = periods.zipWithIndex.map {
    case (value, _) =>
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