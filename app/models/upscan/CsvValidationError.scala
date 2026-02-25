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

package models.upscan

import play.api.libs.json.*


sealed trait CsvColumn { def letter: String; def index: Int }
object CsvColumn {
  case object A extends CsvColumn { val letter = "A"; val index = 1 } // Country
  case object B extends CsvColumn { val letter = "B"; val index = 2 } // VAT rate
  case object C extends CsvColumn { val letter = "C"; val index = 3 } // Sales
  case object D extends CsvColumn { val letter = "D"; val index = 4 } // VAT
}

sealed trait CsvError { def row: Int; def column: CsvColumn; def cellRef: String = s"${column.letter}$row" }

object CsvError {
  final case class InvalidCountry(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class InvalidCharacter(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class InvalidNumberFormat(row: Int, column: CsvColumn, value: String) extends CsvError
  final case class NegativeNumber(row: Int, column: CsvColumn, value: BigDecimal) extends CsvError
  final case class BlankCell(row: Int, column: CsvColumn) extends CsvError
  final case class VatRateNotAllowed(row: Int, column: CsvColumn, country: String, value: String) extends CsvError
}

final case class CsvValidationException(errors: Seq[CsvError])
  extends RuntimeException(s"CSV validation error at ${errors.map(_.cellRef).mkString(", ")}")

implicit val csvColumnFormat: Format[CsvColumn] = new Format[CsvColumn] {
  def writes(o: CsvColumn): JsValue = JsString(o.letter)
  def reads(json: JsValue): JsResult[CsvColumn] = json match {
    case JsString("A") => JsSuccess(CsvColumn.A)
    case JsString("B") => JsSuccess(CsvColumn.B)
    case JsString("C") => JsSuccess(CsvColumn.C)
    case JsString("D") => JsSuccess(CsvColumn.D)
    case _ => JsError("Unknown column")
  }
}

implicit val csvErrorFormat: Format[CsvError] = {
  val invalidCountry = Json.format[CsvError.InvalidCountry]
  val invalidCharacter = Json.format[CsvError.InvalidCharacter]
  val invalidNumber = Json.format[CsvError.InvalidNumberFormat]
  val negative = Json.format[CsvError.NegativeNumber]
  val blank = Json.format[CsvError.BlankCell]
  val vat = Json.format[CsvError.VatRateNotAllowed]

  new Format[CsvError] {

    def writes(o: CsvError): JsObject = o match {
      case e: CsvError.InvalidCountry      => invalidCountry.writes(e) + ("type" -> JsString("InvalidCountry"))
      case e: CsvError.InvalidCharacter    => invalidCharacter.writes(e) + ("type" -> JsString("InvalidCharacter"))
      case e: CsvError.InvalidNumberFormat => invalidNumber.writes(e) + ("type" -> JsString("InvalidNumberFormat"))
      case e: CsvError.NegativeNumber      => negative.writes(e) + ("type" -> JsString("NegativeNumber"))
      case e: CsvError.BlankCell           => blank.writes(e) + ("type" -> JsString("BlankCell"))
      case e: CsvError.VatRateNotAllowed   => vat.writes(e) + ("type" -> JsString("VatRateNotAllowed"))
    }

    def reads(json: JsValue): JsResult[CsvError] =
      (json \ "type").validate[String].flatMap {
        case "InvalidCountry"      => invalidCountry.reads(json)
        case "InvalidCharacter"    => invalidCharacter.reads(json)
        case "InvalidNumberFormat" => invalidNumber.reads(json)
        case "NegativeNumber"      => negative.reads(json)
        case "BlankCell"           => blank.reads(json)
        case "VatRateNotAllowed"   => vat.reads(json)
        case other                 => JsError(s"Unknown type $other")
      }
  }
}