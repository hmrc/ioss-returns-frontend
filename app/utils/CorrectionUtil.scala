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

package utils

import models.etmp.EtmpVatReturn

object CorrectionUtil {

  def calculateNegativeAndZeroCorrections(etmpVatReturn: EtmpVatReturn): Map[String, BigDecimal] = {
    val allGoodsSuppliedAmountsForCountry = etmpVatReturn.goodsSupplied.groupBy(_.msOfConsumption).flatMap {
      case (country, goodsSuppliedSales) =>
        val totalAmount = goodsSuppliedSales.map(_.vatAmountGBP).sum

        Map(country -> totalAmount)
    }

    val allCorrectionAmountsForCountry = etmpVatReturn.correctionPreviousVATReturn.groupBy(_.msOfConsumption).flatMap {
      case (country, corrections) =>
        val totalCorrectionAmount = corrections.map(_.totalVATAmountCorrectionGBP).sum

        Map(country -> totalCorrectionAmount)
    }

    val countryList = (allGoodsSuppliedAmountsForCountry.keys ++ allCorrectionAmountsForCountry.keys).toList.distinct

    countryList.flatMap { country =>
      val goodsSuppliedAmountForCountry = allGoodsSuppliedAmountsForCountry.getOrElse(country, BigDecimal(0))
      val correctionsAmountForCountry = allCorrectionAmountsForCountry.getOrElse(country, BigDecimal(0))

      Map(country -> (goodsSuppliedAmountForCountry + correctionsAmountForCountry))
    }.toMap.filter {
      case (_, vatAmount) =>
        vatAmount <= 0
    }
  }
}
