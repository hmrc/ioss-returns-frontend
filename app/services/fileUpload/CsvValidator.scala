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

package services.fileUpload

import models.upscan.{CsvColumn, CsvError, CsvValidationException}
import models.{Country, Period}
import services.VatRateService
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class CsvValidator @Inject()(vatRateService: VatRateService)(implicit ec: ExecutionContext) {

  private val VatRateRegex = """^\s*-?\d+(?:\.\d{1,2})?\s*%?\s*$""".r
  private val CountryAllowedCharsRegex = """^[\p{L}\p{M}\s'.-]+$""".r
  private val MaxColumns = 4

  def validateOrThrow(
                       rows: Seq[Array[String]],
                       period: Period
                     )(implicit hc: HeaderCarrier): Future[Unit] = {
    validate(rows, period).map { errors =>
      if (errors.nonEmpty) throw CsvValidationException(errors)
    }
  }

  private def validate(
                        rows: Seq[Array[String]],
                        period: Period
                      )(implicit hc: HeaderCarrier): Future[Seq[CsvError]] = {
    val headerIndex = rows.indexWhere { row =>
      row.headOption.exists(c => c.replace("\"", "").trim.equalsIgnoreCase("Country"))
    }

    if (headerIndex < 0) {
      Seq(CsvError.InvalidCountry(row = 1, column = CsvColumn.A, value = "Missing header 'Country'")).toFuture
    } else {
      val dataRows = rows.drop(headerIndex + 1).filterNot(isRowEmpty)

      val basicErrors: Seq[CsvError] =
        dataRows.zipWithIndex.flatMap { case (row, idx0) =>
          val csvRowNumber = (headerIndex + 2) + idx0
          validateRow(csvRowNumber, row)
        }

      val duplicateErrors: Seq[CsvError] =
        validateDuplicateCountryVatRate(dataRows, headerIndex)

      validateVatRatesAllowed(dataRows, headerIndex, period).map { vatRateErrors =>
        basicErrors ++ duplicateErrors ++ vatRateErrors
      }
    }
  }

  private def validateRow(csvRowNumber: Int, row: Array[String]): Seq[CsvError] = {

    val countryRaw = cell(row, 0)
    val vatRateRaw = cell(row, 1)
    val salesRaw   = cell(row, 2)
    val vatRaw     = cell(row, 3)

    validateColumnCount(csvRowNumber, row) ++
      validateCountry(csvRowNumber, countryRaw) ++
      validateVatRate(csvRowNumber, vatRateRaw) ++
      validateMoney(csvRowNumber, CsvColumn.C, salesRaw) ++
      validateMoney(csvRowNumber, CsvColumn.D, vatRaw)
  }

  private def validateCountry(row: Int, countryRaw: String): Seq[CsvError] = {
    if (countryRaw.isEmpty) {
      Seq(CsvError.BlankCell(row, CsvColumn.A))
    } else if (!CountryAllowedCharsRegex.matches(countryRaw)) {
      Seq(CsvError.InvalidCharacter(row, CsvColumn.A, countryRaw))
    } else {
      val exists = Country.euCountriesWithNI.exists(_.name.equalsIgnoreCase(countryRaw))
      if (!exists) Seq(CsvError.InvalidCountry(row, CsvColumn.A, countryRaw)) else Nil
    }
  }

  private def validateVatRate(row: Int, vatRateRaw: String): Seq[CsvError] = {
    if (vatRateRaw.isEmpty) {
      Seq(CsvError.BlankCell(row, CsvColumn.B))
    } else if (!VatRateRegex.matches(vatRateRaw)) {
      Seq(CsvError.InvalidNumberFormat(row, CsvColumn.B, vatRateRaw))
    } else {
      val cleaned = vatRateRaw.replace("%", "").trim
      Try(BigDecimal(cleaned)).toEither match {
        case Left(_) => Seq(CsvError.InvalidNumberFormat(row, CsvColumn.B, vatRateRaw))
        case Right(rate) =>
          if (rate < 0) Seq(CsvError.NegativeNumber(row, CsvColumn.B, rate)) else Nil
      }
    }
  }

  private def validateMoney(row: Int, col: CsvColumn, raw: String): Seq[CsvError] = {
    if (raw.isEmpty) {
      Seq(CsvError.BlankCell(row, col))
    } else {
      val cleaned = raw
        .replace("Â", "")
        .replace("£", "")
        .replace(",", "")
        .trim

      Try(BigDecimal(cleaned)).toEither match {
        case Left(_) => Seq(CsvError.InvalidNumberFormat(row, col, raw))
        case Right(n) =>
          if (n < 0) Seq(CsvError.NegativeNumber(row, col, n)) else Nil
      }
    }
  }

  private def validateVatRatesAllowed(
                                       dataRows: Seq[Array[String]],
                                       headerIndex: Int,
                                       period: Period
                                     )(implicit hc: HeaderCarrier): Future[Seq[CsvError]] = {

    val rowsByCountry = {
      dataRows.zipWithIndex.flatMap {
        case (row, index0) =>
          val countryRaw = cell(row, 0)

          Country.euCountriesWithNI
            .find(_.name.equalsIgnoreCase(countryRaw))
            .map {country =>
              val csvRowNumber = (headerIndex + 2) + index0
              country -> (csvRowNumber -> row)
            }
      }.groupBy(_._1).view.mapValues(_.map(_._2)).toMap
    }

    Future.traverse(rowsByCountry.toSeq) {
      case (country, rows) =>
        vatRateService.vatRates(period, country).map { allowed =>
          val allowedRates = allowed.map(_.rate).toSet

          rows.flatMap {
            case (rowNum, row) =>
              val vatRateRaw = cell(row, 1)

              parseRate(vatRateRaw).toOption match {
                case Some(rate) if !allowedRates.contains(rate) =>
                  Seq(CsvError.VatRateNotAllowed(rowNum, CsvColumn.B, country.name, vatRateRaw))
                case _ =>
                  Nil
              }
          }
        }
    }.map(_.flatten)
  }

  private def validateDuplicateCountryVatRate(
                                               dataRows: Seq[Array[String]],
                                               headerIndex: Int
                                             ): Seq[CsvError] = {

    type CountryWithRate = (String, BigDecimal)

    val (_, errors) = dataRows.zipWithIndex.foldLeft(Map.empty[CountryWithRate, Int] -> Vector.empty[CsvError]) {
      case ((seen, errs), (row, index0)) =>
        val csvRowNumber = (headerIndex +2) + index0
        val countryRaw = cell(row, 0)
        val vatRateRaw = cell(row, 1)

        val countryOpt = Country.euCountriesWithNI.find(_.name.equalsIgnoreCase(countryRaw)).map(_.name)

        val vatRateOpt = parseRate(vatRateRaw)
          .toOption
          .filter(_ >= 0)
          .map(_.bigDecimal.stripTrailingZeros())
          .map(BigDecimal(_))

        (countryOpt, vatRateOpt) match {
          case (Some(country), Some(vatRate)) =>
            val countryRow: CountryWithRate = (country, vatRate)

            if (seen.contains(countryRow)) {
              seen -> (errs :+ CsvError.DuplicateVatRate(csvRowNumber, CsvColumn.B, country, vatRateRaw))
            } else {
              seen.updated(countryRow, csvRowNumber) -> errs
            }

          case _ =>
            seen -> errs
        }
    }
    errors
  }

  private def validateColumnCount(rowNum: Int, row: Array[String]): Seq[CsvError] = {
    val hasExtraNonEmpty =
      row.drop(MaxColumns).exists(cell => cell.replace("\"", "").trim.nonEmpty)

    if (hasExtraNonEmpty) {
      Seq(CsvError.TooManyColumns(rowNum, CsvColumn.D, actualColumns = row.length))
    } else {
      Nil
    }
  }

  
  private def parseRate(raw: String): Either[Unit, BigDecimal] = {
    if (raw.trim.isEmpty) {
      Left(())
    } else if (!VatRateRegex.matches(raw.trim)) {
      Left(())
    } else {
      Try(BigDecimal(raw.replace("%", "").trim)).toEither match {
        case Right(rate) if rate >= 0 => Right(rate)
        case _ => Left(())
      }
    }
  }

  private def isRowEmpty(row: Array[String]): Boolean = {
    row.forall(_.trim.isEmpty)
  }

  private def cell(row: Array[String], i: Int): String = row.lift(i).map(_.replace("\"", "").trim).getOrElse("")
}