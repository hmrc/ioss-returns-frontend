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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import models.{Enumerable, Period}

import java.time.Month
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private[mappings] def stringFormatter(errorKey: String, args: Seq[String] = Seq.empty): Formatter[String] = new Formatter[String] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
      data.get(key) match {
        case None                      => Left(Seq(FormError(key, errorKey, args)))
        case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
        case Some(s)                   => Right(s)
      }

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value)
  }

  private[mappings] def booleanFormatter(requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .flatMap {
          case "true"  => Right(true)
          case "false" => Right(false)
          case _       => Left(Seq(FormError(key, invalidKey, args)))
        }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] def intFormatter(requiredKey: String, wholeNumberKey: String, nonNumericKey: String, args: Seq[String] = Seq.empty): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s =>
              nonFatalCatch
                .either(s.toInt)
                .left.map(_ => Seq(FormError(key, nonNumericKey, args)))
        }

      override def unbind(key: String, value: Int) =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def currencyFormatter(requiredKey: String, invalidNumericKey: String, nonNumericKey: String, decimalCount: Int,
                                          args: Seq[String] = Seq.empty): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      val isdp = """(^-?\d*$)|(^-?\d*\.\d{1,""" + decimalCount + """}$)"""
      val validNumeric = """(^-?\d*$)|(^-?\d*\.\d*$)"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").replaceAll("[,£ ]", "").replace(" ", ""))
          .flatMap {
            case s if !s.matches(validNumeric) =>
              Left(Seq(FormError(key, nonNumericKey, args)))
            case s if !s.matches(isdp) =>
              Left(Seq(FormError(key, invalidNumericKey, args)))
            case s =>
              nonFatalCatch
                .either(BigDecimal(s))
                .left.map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(implicit ev: Enumerable[A]): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap {
          str =>
            ev.withName(str)
              .map(Right.apply)
              .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def periodFormatter(requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty): Formatter[Period] =
    new Formatter[Period] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Period] =
        baseFormatter
          .bind(key, data)
          .flatMap { s => Period.fromString(s).toRight(Seq(FormError(key, invalidKey, args))) }

      def unbind(key: String, value: Period) = Map(key -> value.toString)
    }

  private[mappings] def monthFormatter(requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty): Formatter[Month] =
    new Formatter[Month] {

      private val baseFormatter = stringFormatter(requiredKey, args)
      private val monthFormat = DateTimeFormatter.ofPattern("MMMM")

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Month] =
        baseFormatter
          .bind(key,data)
          .map { value =>
            try {
              Right(Month.from(monthFormat.parse(value)))
            } catch {
              case _: DateTimeParseException =>
                Left(Seq(FormError(key, invalidKey, args)))
            }
          } .getOrElse(Left(Seq(FormError(key, invalidKey, args))))

      override def unbind(key: String, value: Month): Map[String, String] = Map(key -> monthFormat.format(value))
    }
}
