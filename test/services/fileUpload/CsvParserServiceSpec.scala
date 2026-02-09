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
import models.Index
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import pages.*

class CsvParserServiceSpec extends SpecBase with MockitoSugar with Matchers with BeforeAndAfterEach {

  "CSV Parser must" - {

    "parse where the string is empty" in {
      CsvParserService.split("") mustBe Seq()
    }

    "parse for a simple string of elements without quotes with multiple rows in the CSV with no quotes" in {
      val validCSVContent: String =
        """"HM Revenue and Customs logo","","",""
          |"Import One Stop Shop VAT return","","",""
          |"Country","VAT % rate","Total eligible sales","Total VAT due"
          |"Germany","12.50%","£1200","£140"
          |"France","15","33,333","£4423"
          |"France","10%","150.01","£15"
          |""".stripMargin

      val actual: Seq[Seq[String]] = CsvParserService.split(validCSVContent).map(_.toSeq)
      val expected: Seq[Seq[String]] = Seq(
        Seq("HM Revenue and Customs logo", "", "", ""),
        Seq("Import One Stop Shop VAT return", "", "", ""),
        Seq("Country", "VAT % rate", "Total eligible sales", "Total VAT due"),
        Seq("Germany", "12.50%", "£1200", "£140"),
        Seq("France", "15", "33,333", "£4423"),
        Seq("France", "10%", "150.01", "£15")
      )

      actual mustBe expected
    }

    "populate user answers from CSV" in {

      val csv =
        """"HM Revenue and Customs logo","","",""
          |"Import One Stop Shop VAT return","","",""
          |"Country","VAT % rate","Total eligible sales","Total VAT due"
          |"Germany","12.50%","£1200","£140"
          |"France","15","33,333","£4423"
          |"France","10%","150.01","£15"
          |""".stripMargin

      val service = new CsvParserService()
      val result = service.populateUserAnswersFromCsv(emptyUserAnswers, csv)

      result.isSuccess mustBe true
      val updated = result.get

      updated.get(SoldToCountryPage(Index(0))).value.name mustBe "France"
      updated.get(SalesToCountryPage(Index(0), Index(0))).value mustBe BigDecimal(33333)
      updated.get(VatOnSalesPage(Index(0), Index(0))).value.amount mustBe BigDecimal(4423)

      updated.get(SoldToCountryPage(Index(0))).value.name mustBe "France"
      updated.get(SalesToCountryPage(Index(0), Index(1))).value mustBe BigDecimal(150.01)
      updated.get(VatOnSalesPage(Index(0), Index(1))).value.amount mustBe BigDecimal(15)

      updated.get(SoldToCountryPage(Index(1))).value.name mustBe "Germany"
      updated.get(SalesToCountryPage(Index(1), Index(0))).value mustBe BigDecimal(1200)
      updated.get(VatOnSalesPage(Index(1), Index(0))).value.amount mustBe BigDecimal(140)


    }

  }
}
