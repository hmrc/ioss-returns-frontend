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

import base.SpecBase
import models.upscan.{CsvError, CsvValidationException}
import models.{Country, VatRateFromCountry, VatRateType}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import services.VatRateService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CsvValidatorSpec extends SpecBase with MockitoSugar with Matchers with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockVatRateService = mock[VatRateService]
  private val validator = new CsvValidator(mockVatRateService)

  private def rows(csv: String): Seq[Array[String]] = CsvParserService.split(csv)


  private val validCSVContent: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidCSVCountry: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","33,333","£4423"
      |"Frunce","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidCharacterCsv: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","33,333","@"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidNumberCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","33,333","£4423"
      |"France","10%","150.01.098","£15"
      |""".stripMargin
  }

  private val invalidEmptyCellCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidVatRateCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","12.50%","£1200","£140"
      |"France","13","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidMultipleCSV: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","12.50%","£1200","£140"
      |"France","13","33,333",""
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidDuplicateVatRate: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"France","13","33,333","£4423"
      |"France","13%","150.01","£15"
      |""".stripMargin
  }

  private val invalidContentWrongPlace: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140"
      |"13","France","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidColumnSize: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19%","£1200","£140","45"
      |"France","13","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  private val invalidVatRateTwoDecimalPlaces: String = {
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","19.00567%","£1200","£140",
      |"France","13","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
  }

  override def beforeEach(): Unit = {
    reset(mockVatRateService)
    super.beforeEach()

    when(mockVatRateService.vatRates(eqTo(period), argThat[Country](_.name.equalsIgnoreCase("Germany")))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq(
        VatRateFromCountry(BigDecimal(19), VatRateType.Standard, period.firstDay),
        VatRateFromCountry(BigDecimal(7), VatRateType.Reduced, period.firstDay)
      )))

    when(mockVatRateService.vatRates(eqTo(period), argThat[Country](_.name.equalsIgnoreCase("France")))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Seq(
        VatRateFromCountry(BigDecimal(13), VatRateType.Standard, period.firstDay),
        VatRateFromCountry(BigDecimal(10), VatRateType.Reduced, period.firstDay),
        VatRateFromCountry(BigDecimal(5.5), VatRateType.Reduced, period.firstDay)
      )))
  }

  "CSV Validator must" - {

    "CsvValidator.validateOrThrow" - {

      "succeed for a valid CSV" in {

        whenReady(validator.validateOrThrow(rows(validCSVContent), period)) { _ =>
          succeed
        }
      }

      "fail with InvalidCountry for an unknown country (A cell)" in {

        val validatorError = validator.validateOrThrow(rows(invalidCSVCountry), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidCountry => e.cellRef } must contain("A6")
        }

      }

      "fail with InvalidNumberFormat for an invalid money cell" in {

        val validatorError = validator.validateOrThrow(rows(invalidNumberCSV), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("C6")
        }

      }

      "fail with BlankCell when a money cell is empty" in {

        val validatorError = validator.validateOrThrow(rows(invalidEmptyCellCSV), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.BlankCell => e.cellRef } must contain("C5")
        }

      }

      "fail with InvalidNumberFormat when inappropriate symbols appear in money cell" in {

        val validatorError = validator.validateOrThrow(rows(invalidCharacterCsv), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("D5")
        }
      }

      "fail with VatRateNotAllowed when VAT rate is not allowed for that country/period" in {

        val validatorError = validator.validateOrThrow(rows(invalidVatRateCSV), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.VatRateNotAllowed => e.cellRef } must contain("B4")
        }

      }

      "fail with DuplicateVatRate when VAT rate is duplicated for same country" in {

        val validatorError = validator.validateOrThrow(rows(invalidDuplicateVatRate), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.DuplicateVatRate => e.cellRef } must contain("B6")
        }

      }

      "fail with multiple errors when data in wrong position" in {

        val validatorError = validator.validateOrThrow(rows(invalidContentWrongPlace), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.map(_.cellRef) must contain allOf("A5", "B5")
        }
      }

      "fail with TooManyColumns when there is more than 4 columns" in {

        val validatorError = validator.validateOrThrow(rows(invalidColumnSize), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.TooManyColumns => e.cellRef } must contain("D4")
        }

      }

      "fail with InvalidNumberFormat for VAT rate with more than two decimal places" in {

        val validatorError = validator.validateOrThrow(rows(invalidVatRateTwoDecimalPlaces), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.collect { case e: CsvError.InvalidNumberFormat => e.cellRef } must contain("B4")
        }

      }

      "fail with multiple errors when multiple things are wrong" in {

        val validatorError = validator.validateOrThrow(rows(invalidMultipleCSV), period)

        whenReady(validatorError.failed) { ex =>
          ex mustBe a[CsvValidationException]
          val errors = ex.asInstanceOf[CsvValidationException].errors

          errors.map(_.cellRef) must contain allOf ("B4", "D5")
        }
      }
    }
  }
}
