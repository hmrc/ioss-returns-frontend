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
import models.{Country, Index, UserAnswers, VatOnSales, VatRatesFromCountry}
import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import pages._
import queries.SalesByCountryQuery

class EligibleSalesJourneySpec extends AnyFreeSpec with JourneyHelpers with SpecBase with Generators {

  private val maxCountries: Int = Country.euCountriesWithNI.size
  private val minSalesValue: Int = 1
  private val maxSalesValue: Int = 1000000
  private val countryIndex1: Index = Index(0)
  private val countryIndex2: Index = Index(1)
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val salesValue: Int = Gen.chooseNum(minSalesValue,maxSalesValue).sample.value

  private val initialAnswers = UserAnswers(userAnswersId, period)

  private val initialise = journeyOf(
    submitAnswer(SoldGoodsPage, true),
    submitAnswer(SoldToCountryPage(countryIndex1), country),
    submitAnswer(VatRatesFromCountryPage(countryIndex1), Set(VatRatesFromCountry.values.head)),
    submitAnswer(SalesToCountryPage(countryIndex1), salesValue),
    submitAnswer(VatOnSalesPage(countryIndex1), VatOnSales.values.head),
    pageMustBe(SoldToCountryListPage(Some(countryIndex1)))
  )


  "must go directly to Check your answers if no eligible sales were made" in {
    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage, false),
        pageMustBe(CheckYourAnswersPage) // TODO -> To correct a previous return page when created
      )
  }

  s"must be asked for as many as necessary upto the maximum of $maxCountries EU countries" in {

    def generateSales: Seq[JourneyStep[Unit]] = {
      (0 until maxCountries).foldLeft(Seq.empty[JourneyStep[Unit]]) {
        case (journeySteps: Seq[JourneyStep[Unit]], index: Int) =>
          journeySteps :+
            submitAnswer(SoldToCountryPage(Index(index)), country) :+
            submitAnswer(VatRatesFromCountryPage(Index(index)), Set(arbitraryVatRatesFromCountry.arbitrary.sample.value)) :+
            submitAnswer(SalesToCountryPage(Index(index)), salesValue) :+
            submitAnswer(VatOnSalesPage(Index(index)), arbitraryVatOnSales.arbitrary.sample.value) :+
            pageMustBe(SoldToCountryListPage(Some(Index(index)))) :+
            goTo(SoldToCountryListPage(Some(Index(index)))) :+
            submitAnswer(SoldToCountryListPage(Some(Index(index))), true)
      }
    }

    startingFrom(SoldGoodsPage, answers = initialAnswers)
      .run(
        submitAnswer(SoldGoodsPage, true) +:
          generateSales :+
          pageMustBe(CheckYourAnswersPage): _*
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
        submitAnswer(VatRatesFromCountryPage(countryIndex2), Set(VatRatesFromCountry.values.head)),
        submitAnswer(SalesToCountryPage(countryIndex2), salesValue),
        submitAnswer(VatOnSalesPage(countryIndex2), VatOnSales.values.head),
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
          submitAnswer(VatRatesFromCountryPage(countryIndex2), Set(VatRatesFromCountry.values.head)),
          submitAnswer(SalesToCountryPage(countryIndex2), salesValue),
          submitAnswer(VatOnSalesPage(countryIndex2), VatOnSales.values.head),
          pageMustBe(SoldToCountryListPage(Some(countryIndex2))),
          goTo(DeleteSoldToCountryPage(countryIndex2)),
          removeAddToListItem(SalesByCountryQuery(countryIndex2)),
          pageMustBe(SoldToCountryListPage()),
          answersMustNotContain(SalesByCountryQuery(countryIndex2))
        )
    }
  }

  // TODO Change answers journey
  // TODO Delete all answers journey from CYA page
}