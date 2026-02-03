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

import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import models.VatRateType.Standard
import models.{Country, Index, UserAnswers, VatOnSales, VatOnSalesChoice, VatRateFromCountry}
import pages.{SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}

import java.io.*
import java.time.LocalDate
import scala.jdk.CollectionConverters.ListHasAsScala
import javax.inject.{Inject, Singleton}
import scala.util.Try

object CsvParserService {
  
  def split(content: String): Seq[Array[String]] = {
    val settings = new CsvParserSettings()
    settings.setNullValue("")
    settings.setEmptyValue("")
    settings.setSkipEmptyLines(true)
    settings.setIgnoreLeadingWhitespaces(true)
    settings.setIgnoreTrailingWhitespaces(true)
    
    val parser = new CsvParser(settings)

    val rows: Seq[Array[String]] =
      parser.parseAll(new StringReader(content)).asScala.toSeq
    
    removeNonPrintableChars(rows)
  }

  private def removeNonPrintableChars(csvContent: Seq[Array[String]]): Seq[Array[String]] = {
    csvContent.map(_.map(_.replaceAll("\\p{C}", "")))
  }
}

@Singleton

class CsvParserService @Inject()() {
  
  private final case class VatRow(
                           country: String,
                           vatRate: String,
                           salesToCountry: BigDecimal,
                           vatOnSales: BigDecimal
                         )
  
  private val inlineCsv: String = 
    """"HM Revenue and Customs logo","","",""
      |"Import One Stop Shop VAT return","","",""
      |"Country","VAT % rate","Total eligible sales","Total VAT due"
      |"Germany","12.50%","£1200","£140"
      |"France","15","33,333","£4423"
      |"France","10%","150.01","£15"
      |""".stripMargin
      
  def populateUserAnswersFromCsv(userAnswers: UserAnswers): Try[UserAnswers] = {
    val rawRows: Seq[Array[String]] = CsvParserService.split(inlineCsv)
    val parsedRows: Seq[VatRow] = extractVatRows(rawRows)
    val soldGoodsAnswer: Try[UserAnswers] = userAnswers.set(SoldGoodsPage, true)

    parsedRows.zipWithIndex.foldLeft(soldGoodsAnswer) {
      case (accTry, (row, index)) =>
        val countryIndex = Index(index)
        val vatRateIndex = Index(0)
       
        accTry.flatMap { ua =>
          ua
            .set(SoldToCountryPage(countryIndex), countryFromName(row.country))
            .flatMap(_.set(VatRatesFromCountryPage(countryIndex, vatRateIndex), List(vatRateFrom(row.vatRate))))
            .flatMap(_.set(SalesToCountryPage(countryIndex, vatRateIndex), row.salesToCountry))
            .flatMap(_.set(VatOnSalesPage(countryIndex, vatRateIndex), vatOnSalesFrom(row.vatOnSales)))
        }
    }
  }
  
  private def extractVatRows(rows: Seq[Array[String]]): Seq[VatRow] = {
    
    val headerIndex = {
      rows.indexWhere(_.headOption.exists(_.trim.equalsIgnoreCase("Country")))
    }
    
    if (headerIndex < 0) {
      Seq.empty
    } else {
      rows
        .drop(headerIndex + 1)
        .collect {
          case Array(country, vatRate, sales, vat) =>
            VatRow(
              country = country,
              vatRate = parseVatRate(vatRate),
              salesToCountry = parseValue(sales),
              vatOnSales = parseValue(vat)
            )
        }
    }
  }
  
  private def parseVatRate(vatRateFromCsv: String): String = {
    String(vatRateFromCsv.replace("%", "").trim)
  }

  private def parseValue(valueFromCsv: String): BigDecimal = {
    BigDecimal(valueFromCsv.replace("£", "").replace(",", "").trim)
  }
  
  private def countryFromName(countryName: String): Country = {
    Country.euCountries.find(_.name == countryName)
      .getOrElse(throw new IllegalArgumentException(s"Unknown country: '$countryName'"))
  }
  
  private def vatRateFrom(vatRate: String): VatRateFromCountry = {
    VatRateFromCountry(
      rate = BigDecimal(vatRate),
      rateType = Standard,
      validFrom = LocalDate.parse("2021-01-01")
    )
  }
  
  private def vatOnSalesFrom(amount: BigDecimal): VatOnSales = {
    VatOnSales(
      choice = VatOnSalesChoice.Standard,
      amount = amount
    )
  }
  
}
