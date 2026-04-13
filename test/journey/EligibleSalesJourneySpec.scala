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

package journey

import base.SpecBase
import generators.Generators
import models.{Country, Index, UserAnswers, VatRateFromCountry}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import pages.*
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

  private val initialAnswers = UserAnswers(userAnswersId, iossNumber, period)

  private val initialise = journeyOf(
    submitAnswer(SoldGoodsPage(iossNumber), true),
    submitAnswer(SoldToCountryPage(iossNumber, countryIndex1), country),
    submitAnswer(VatRatesFromCountryPage(iossNumber, countryIndex1, vatRateIndex), List(vatRateFromCountry1, vatRateFromCountry2)),
    submitAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex), salesValue),
    submitAnswer(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
    submitAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex. +(1)), salesValue),
    submitAnswer(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value),
    pageMustBe(CheckSalesPage(iossNumber, countryIndex1)),
    submitAnswer(CheckSalesPage(iossNumber, countryIndex1),false),
    pageMustBe(SoldToCountryListPage(iossNumber))
  )

  "must go directly to Check your answers if no eligible sales were made" in {
    startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage(iossNumber), false),
        pageMustBe(CorrectPreviousReturnPage(iossNumber, 0))
      )
  }

  s"must be asked for as many as necessary upto the maximum of $maxCountries EU countries" in {

    def generateSales: Seq[JourneyStep[Unit]] = {
      (0 until  maxCountries).foldLeft(Seq.empty[JourneyStep[Unit]]) {
        case (journeySteps: Seq[JourneyStep[Unit]], index: Int) =>
          journeySteps :+
            submitAnswer(SoldToCountryPage(iossNumber, Index(index)), country) :+
            submitAnswer(VatRatesFromCountryPage(iossNumber, Index(index), vatRateIndex), List(vatRateFromCountry1, vatRateFromCountry2)) :+
            submitAnswer(SalesToCountryPage(iossNumber, Index(index), vatRateIndex), salesValue) :+
            submitAnswer(VatOnSalesPage(iossNumber, Index(index), vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value) :+
            pageMustBe(SalesToCountryPage(iossNumber, Index(index), vatRateIndex. +(1))) :+
            submitAnswer(SalesToCountryPage(iossNumber, Index(index), vatRateIndex. +(1)), salesValue) :+
            submitAnswer(VatOnSalesPage(iossNumber, Index(index), vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value) :+
            submitAnswer(CheckSalesPage(iossNumber, Index(index)), false) :+
            submitAnswer(SoldToCountryListPage(iossNumber), index != maxCountries -1)
      }
    }

    startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage(iossNumber), true) +:
          generateSales :+
          pageMustBe(CorrectPreviousReturnPage(iossNumber, 0)): _*
      )
  }

  "must allow the user to add a sale" in {
    startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
      .run(
        initialise
      )
  }

  "must allow the user to add more than one sale" in {
    startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
      .run(
        initialise,
        submitAnswer(SoldToCountryListPage(iossNumber), true),
        submitAnswer(SoldToCountryPage(iossNumber, countryIndex2), country),
        submitAnswer(VatRatesFromCountryPage(iossNumber, countryIndex2, vatRateIndex), List(vatRateFromCountry1, vatRateFromCountry2, vatRateFromCountry3)),
        submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex), salesValue),
        submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
        submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1)), salesValue),
        submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value),
        submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(2)), salesValue),
        submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(2)), arbitraryVatOnSales.arbitrary.sample.value),
        pageMustBe(CheckSalesPage(iossNumber, countryIndex2)),
        submitAnswer(CheckSalesPage(iossNumber, countryIndex2), false),
        pageMustBe(SoldToCountryListPage(iossNumber))
      )
  }

  "must be able to remove them" - {

    "when there is only one" in {
      startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
        .run(
          initialise,
          goTo(DeleteSoldToCountryPage(iossNumber, countryIndex1)),
          removeAddToListItem(SalesByCountryQuery(countryIndex1)),
          pageMustBe(SoldGoodsPage(iossNumber)),
          answersMustNotContain(SalesByCountryQuery(countryIndex1))
        )
    }

    "when there are multiple" in {
      startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
        .run(
          initialise,
          submitAnswer(SoldToCountryListPage(iossNumber), true),
          submitAnswer(SoldToCountryPage(iossNumber, countryIndex2), country),
          submitAnswer(VatRatesFromCountryPage(iossNumber, countryIndex2, vatRateIndex), List(vatRateFromCountry1, vatRateFromCountry2)),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex), salesValue),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1)), salesValue),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(CheckSalesPage(iossNumber, countryIndex2), false),
          pageMustBe(SoldToCountryListPage(iossNumber)),
          goTo(DeleteSoldToCountryPage(iossNumber, countryIndex2)),
          removeAddToListItem(SalesByCountryQuery(countryIndex2)),
          pageMustBe(SoldToCountryListPage(iossNumber)),
          answersMustNotContain(SalesByCountryQuery(countryIndex2))
        )
    }
  }

  "must be able to change the users original sales answers" - {

    "when there is only one VAT rate sale" in {

      val changedSalesValue: BigDecimal = Gen.chooseNum(minSalesValue, maxSalesValue).sample.value.toDouble

      startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
        .run(
          initialise,
          pageMustBe(SoldToCountryListPage(iossNumber)),
          goTo(CheckSalesPage(iossNumber, countryIndex1)),
          pageMustBe(CheckSalesPage(iossNumber, countryIndex1)),
          goToChangeAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex)),
          pageMustBe(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex)),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex), changedSalesValue),
          pageMustBe(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex)),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(iossNumber, countryIndex1)),
          answerMustEqual(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex), changedSalesValue)
        )
    }

    "when there are multiple VAT rate sales" in {

      val changedSalesValue: BigDecimal = Gen.chooseNum(minSalesValue, maxSalesValue).sample.value.toDouble

      startingFrom(SoldGoodsPage(iossNumber), answers = initialAnswers)
        .run(
          initialise,
          submitAnswer(SoldToCountryListPage(iossNumber), true),
          submitAnswer(SoldToCountryPage(iossNumber, countryIndex2), country),
          submitAnswer(VatRatesFromCountryPage(iossNumber, countryIndex2, vatRateIndex), List(vatRateFromCountry1, vatRateFromCountry2, vatRateFromCountry3)),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex), salesValue),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1))),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1)), salesValue),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(2)), salesValue),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(2)), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(iossNumber, countryIndex2)),
          goToChangeAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1))),
          pageMustBe(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1))),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1)), changedSalesValue),
          pageMustBe(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(1))),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex2, vatRateIndex. +(1)), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(iossNumber, countryIndex2)),
          answerMustEqual(SalesToCountryPage(iossNumber, countryIndex2, vatRateIndex. +(1)), changedSalesValue),
          goToChangeAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex)),
          pageMustBe(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex)),
          submitAnswer(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex), changedSalesValue),
          pageMustBe(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex)),
          submitAnswer(VatOnSalesPage(iossNumber, countryIndex1, vatRateIndex), arbitraryVatOnSales.arbitrary.sample.value),
          pageMustBe(CheckSalesPage(iossNumber, countryIndex1)),
          answerMustEqual(SalesToCountryPage(iossNumber, countryIndex1, vatRateIndex), changedSalesValue),
          submitAnswer(CheckSalesPage(iossNumber, countryIndex1), false),
          pageMustBe(SoldToCountryListPage(iossNumber))
        )
    }
  }
}