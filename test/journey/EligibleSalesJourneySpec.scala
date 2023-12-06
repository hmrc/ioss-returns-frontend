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

package journey

import base.SpecBase
import generators.Generators
import models.VatOnSalesChoice.Standard
import models.{Country, Index, UserAnswers, VatOnSales, VatRateFromCountry}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import pages._
import pages.corrections.CorrectPreviousReturnPage
import queries.SalesByCountryQuery

class EligibleSalesJourneySpec extends AnyFreeSpec with JourneyHelpers with SpecBase with Generators {

  private val maxCountries: Int = Country.euCountriesWithNI.size
  private val minSalesValue: Int = 1
  private val maxSalesValue: Int = 1000000
  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val vatRateFromCountry1: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val vatRateFromCountry2: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val vatRateFromCountry3: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = Gen.chooseNum(minSalesValue,maxSalesValue).sample.value

  private val initialAnswers = UserAnswers(userAnswersId, period)

  private val initialise = journeyOf(
    submitAnswer(SoldGoodsPage, true),
    submitAnswer(SoldToCountryPage(countryIndex1), country),
    submitAnswer(VatRatesFromCountryPage(countryIndex1), List(vatRateFromCountry1, vatRateFromCountry2)),
    submitAnswer(SalesToCountryPage(countryIndex1, vatRateIndex), salesValue),
    submitAnswer(VatOnSalesPage(countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
    submitAnswer(SalesToCountryPage(countryIndex1, vatRateIndex.+(1)), salesValue),
    submitAnswer(VatOnSalesPage(countryIndex1, vatRateIndex.+(1)), arbitraryVatOnSales.arbitrary.sample.value),
    submitAnswer(CheckSalesPage(Some(countryIndex1)), false),
    pageMustBe(SoldToCountryListPage(Some(countryIndex1)))
  )

  "must go directly to Check your answers if no eligible sales were made" in {
    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage, false),
        pageMustBe(CorrectPreviousReturnPage)
      )
  }

  s"must be asked for as many as necessary upto the maximum of $maxCountries EU countries" in {

    def generateSales: Seq[JourneyStep[Unit]] = {
      (0 until maxCountries).foldLeft(Seq.empty[JourneyStep[Unit]]) {
        case (journeySteps: Seq[JourneyStep[Unit]], index: Int) =>
          journeySteps :+
            submitAnswer(SoldToCountryPage(Index(index)), country) :+
            submitAnswer(VatRatesFromCountryPage(Index(index)), List(vatRateFromCountry1, vatRateFromCountry2)) :+
            submitAnswer(SalesToCountryPage(Index(index), vatRateIndex), salesValue) :+
            submitAnswer(VatOnSalesPage(Index(index), vatRateIndex), VatOnSales(Standard, salesValue * vatRateFromCountry1.rate)) :+
            pageMustBe(SalesToCountryPage(Index(index), vatRateIndex.+(1))) :+
            submitAnswer(SalesToCountryPage(Index(index), vatRateIndex.+(1)), salesValue) :+
            submitAnswer(VatOnSalesPage(Index(index), vatRateIndex.+(1)), VatOnSales(Standard, salesValue * vatRateFromCountry2.rate)) :+
            submitAnswer(CheckSalesPage(Some(Index(index))), false) :+
            submitAnswer(SoldToCountryListPage(Some(Index(index))), true)
      }
    }

    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage, true) +:
          generateSales :+
          pageMustBe(CorrectPreviousReturnPage): _*
      )
  }

  "must allow the user to add a sale" in {
    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        initialise
      )
  }

  "must allow the user to add more than one sale" in {
    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        initialise,
        submitAnswer(SoldToCountryListPage(Some(countryIndex1)), true),
        submitAnswer(SoldToCountryPage(countryIndex2), country),
        submitAnswer(VatRatesFromCountryPage(countryIndex2), List(vatRateFromCountry1, vatRateFromCountry2, vatRateFromCountry3)),
        submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex), salesValue),
        submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
        submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(1)), salesValue),
        submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(1)), arbitraryVatOnSales.arbitrary.sample.value),
        submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(2)), salesValue),
        submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(2)), arbitraryVatOnSales.arbitrary.sample.value),
        submitAnswer(CheckSalesPage(Some(countryIndex2)), false),
        pageMustBe(SoldToCountryListPage(Some(countryIndex2)))
      )
  }

  "must be able to remove them" - {

    "when there is only one" in {
      startingFrom(SoldGoodsPage, answers = initialAnswers)
        .run(
          initialise,
          goTo(DeleteSoldToCountryPage(countryIndex1)),
          removeAddToListItem(SalesByCountryQuery(countryIndex1)),
          pageMustBe(SoldGoodsPage),
          answersMustNotContain(SalesByCountryQuery(countryIndex1))
        )
    }

    "when there are multiple" in {
      startingFrom(SoldGoodsPage, answers = initialAnswers)
        .run(
          initialise,
          submitAnswer(SoldToCountryListPage(Some(countryIndex1)), true),
          submitAnswer(SoldToCountryPage(countryIndex2), country),
          submitAnswer(VatRatesFromCountryPage(countryIndex2), List(vatRateFromCountry1, vatRateFromCountry2)),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(1)), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(1)), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(CheckSalesPage(Some(countryIndex2)), false),
          pageMustBe(SoldToCountryListPage(Some(countryIndex2))),
          goTo(DeleteSoldToCountryPage(countryIndex2)),
          removeAddToListItem(SalesByCountryQuery(countryIndex2)),
          pageMustBe(SoldToCountryListPage()),
          answersMustNotContain(SalesByCountryQuery(countryIndex2))
        )
    }
  }

  "must be able to change the users original sales answers" - {

    "when there is only one VAT rate sale" in {

      val changedSalesValue: BigDecimal = Gen.chooseNum(minSalesValue, maxSalesValue).sample.value.toDouble

      startingFrom(SoldGoodsPage, answers = initialAnswers)
        .run(
          initialise,
          pageMustBe(SoldToCountryListPage(Some(countryIndex1))),
          goTo(CheckSalesPage(Some(countryIndex1))),
          pageMustBe(CheckSalesPage(Some(countryIndex1))),
          goToChangeAnswer(SalesToCountryPage(countryIndex1, vatRateIndex)),
          pageMustBe(SalesToCountryPage(countryIndex1, vatRateIndex)),
          submitAnswer(SalesToCountryPage(countryIndex1, vatRateIndex), changedSalesValue),
          pageMustBe(VatOnSalesPage(countryIndex1, vatRateIndex)),
          submitAnswer(VatOnSalesPage(countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(Some(countryIndex1))),
          answerMustEqual(SalesToCountryPage(countryIndex1, vatRateIndex), changedSalesValue)
        )
    }

    "when there are multiple VAT rate sales" in {

      val changedSalesValue: BigDecimal = Gen.chooseNum(minSalesValue, maxSalesValue).sample.value.toDouble

      startingFrom(SoldGoodsPage, answers = initialAnswers)
        .run(
          initialise,
          submitAnswer(SoldToCountryListPage(Some(countryIndex1)), true),
          submitAnswer(SoldToCountryPage(countryIndex2), country),
          submitAnswer(VatRatesFromCountryPage(countryIndex2), List(vatRateFromCountry1, vatRateFromCountry2, vatRateFromCountry3)),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(SalesToCountryPage(countryIndex2, vatRateIndex.+(1))),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(1)), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(1)), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(2)), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(2)), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(Some(countryIndex2))),
          goToChangeAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(1))),
          pageMustBe(SalesToCountryPage(countryIndex2, vatRateIndex.+(1))),
          submitAnswer(SalesToCountryPage(countryIndex2, vatRateIndex.+(1)), changedSalesValue),
          pageMustBe(VatOnSalesPage(countryIndex2, vatRateIndex.+(1))),
          submitAnswer(VatOnSalesPage(countryIndex2, vatRateIndex.+(1)), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(Some(countryIndex2))),
          answerMustEqual(SalesToCountryPage(countryIndex2, vatRateIndex.+(1)), changedSalesValue),
          goToChangeAnswer(SalesToCountryPage(countryIndex1, vatRateIndex)),
          pageMustBe(SalesToCountryPage(countryIndex1, vatRateIndex)),
          submitAnswer(SalesToCountryPage(countryIndex1, vatRateIndex), changedSalesValue),
          pageMustBe(VatOnSalesPage(countryIndex1, vatRateIndex)),
          submitAnswer(VatOnSalesPage(countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(Some(countryIndex2))),
          answerMustEqual(SalesToCountryPage(countryIndex1, vatRateIndex), changedSalesValue)
        )
    }
  }
}