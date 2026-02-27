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

import base.SpecBase
import config.Constants.{maxCurrencyAmount, minCurrencyAmount}
import controllers.{corrections, routes}
import models.VatRateType.Standard
import models.corrections.CorrectionToCountry
import models.requests.DataRequest
import models.{Country, UserAnswers, VatOnSales, VatOnSalesChoice, VatRateType}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.SoldGoodsPage
import pages.corrections.CorrectPreviousReturnPage
import play.api.mvc.AnyContent
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import queries.*
import testUtils.RegistrationData.{emptyUserAnswers, testCredentials}

import java.time.LocalDate


class CompletionChecksSpec extends SpecBase with MockitoSugar {


  implicit val request: DataRequest[AnyContent] = mock[DataRequest[AnyContent]]
  private val country = arbitrary[Country].sample.value
  private val vatAmountOnSales = 100
  private val completeCorrection = CorrectionToCountry(country, Some(BigDecimal(vatAmountOnSales)))
  private val incompleteCorrection = CorrectionToCountry(country, None)
  private val salesValue: BigDecimal = Gen.chooseNum(minCurrencyAmount, maxCurrencyAmount).sample.value
  private val vatOnSalesValue: VatOnSales = arbitraryVatOnSales.arbitrary.sample.value

  private def dataRequest(userAnswers: UserAnswers): DataRequest[AnyContent] = DataRequest(
    FakeRequest(),
    testCredentials,
    Some(vrn),
    iossNumber,
    companyName,
    registrationWrapper,
    Some(intermediaryNumber),
    userAnswers
  )

  val vatRateAndSalesFromCountry: SalesToCountryWithOptionalSales = SalesToCountryWithOptionalSales(
    country = country,
    vatRatesFromCountry = Some(
      List(
        VatRateWithOptionalSalesFromCountry(
          rate = BigDecimal(21.0),
          rateType = Standard,
          validFrom = period.firstDay,
          validUntil = None,
          salesAtVatRate = Some(OptionalSalesAtVatRate(Some(salesValue), Some(vatOnSalesValue)))
        ),
      )
    )
  )

  val vatRateAndSalesFromCountryIncomplete: SalesToCountryWithOptionalSales = SalesToCountryWithOptionalSales(
    country = country,
    vatRatesFromCountry = Some(
      List(
        VatRateWithOptionalSalesFromCountry(
          rate = BigDecimal(21.0),
          rateType = Standard,
          validFrom = period.firstDay,
          validUntil = None,
          salesAtVatRate = None
        ),
      )
    )
  )


  private def completeVatRate: VatRateWithOptionalSalesFromCountry =
    VatRateWithOptionalSalesFromCountry(
      rate = BigDecimal(20),
      rateType = VatRateType.Standard,
      validFrom = LocalDate.of(2023, 3, 1),
      validUntil = None,
      salesAtVatRate = Some(
        OptionalSalesAtVatRate(
          netValueOfSales = Some(BigDecimal(100)),
          vatOnSales = Some(VatOnSales(VatOnSalesChoice.Standard, BigDecimal(20)))
        )
      )
    )

  private def incompleteVatRateNoSales: VatRateWithOptionalSalesFromCountry =
    VatRateWithOptionalSalesFromCountry(
      rate = BigDecimal(20),
      rateType = VatRateType.Standard,
      validFrom = LocalDate.of(2023, 3, 1),
      validUntil = None,
      salesAtVatRate = None
    )

  private def incompleteVatRateNoVat: VatRateWithOptionalSalesFromCountry =
    VatRateWithOptionalSalesFromCountry(
      rate = BigDecimal(20),
      rateType = VatRateType.Standard,
      validFrom = LocalDate.of(2023, 3, 1),
      validUntil = None,
      salesAtVatRate = Some(
        OptionalSalesAtVatRate(
          netValueOfSales = Some(BigDecimal(100)),
          vatOnSales = None
        )
      )
    )

  object TestCompletionChecks extends CompletionChecks

  "CompletionChecks" - {

    "getIncompleteCorrectionsToCountry" - {

      "return None if all corrections are complete" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectionToCountryQuery(index, index), completeCorrection).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrectionsToCountry(index, index)

          result mustBe None
        }
      }

      "return Some if there are incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectionToCountryQuery(index, index), incompleteCorrection).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrectionsToCountry(index, index)

          result mustBe Some(incompleteCorrection)
        }
      }
    }

    "getIncompleteCorrections" - {

      "return an empty list if all corrections are complete" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(completeCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrections(index)

          result mustBe empty
        }
      }

      "return a list of incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(incompleteCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteCorrections(index)

          result mustBe List(incompleteCorrection)
        }
      }
    }

    "firstIndexedIncompleteCorrection" - {

      "return None if there are no incomplete corrections" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(completeCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCorrection(
            index,
            List(CorrectionToCountry(country, None))
          )

          result mustBe None
        }
      }

      "return the first incomplete correction with its index" in {

        val userAnswers = emptyUserAnswers
          .set(AllCorrectionCountriesQuery(index), List(incompleteCorrection)).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCorrection(index, List(incompleteCorrection))

          result mustBe Some((incompleteCorrection, 0))
        }
      }
    }

    "getIncompleteVatRateAndSales" - {

      "return an empty list if there are no incomplete sales with Vat rate" in {

        val vatRates = Seq(completeVatRate)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithOptionalVatQuery(index), vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteVatRateAndSales(index)

           result mustBe empty
        }

      }

      "return a list of incomplete corrections if there are no sales" in {
        val vatRates = Seq(incompleteVatRateNoSales)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithOptionalVatQuery(index), vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteVatRateAndSales(index)

           result mustBe Seq((incompleteVatRateNoSales, 0))
        }
      }

      "return a list of incomplete corrections if there is no vat" in {
        val vatRates = Seq(incompleteVatRateNoVat)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithOptionalVatQuery(index), vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getIncompleteVatRateAndSales(index)

          result mustBe Seq((incompleteVatRateNoVat, 0))
        }
      }
    }

    "getCountriesWithIncompleteSales" - {

      "return and empty sequence if there are no incomplete sales from any country" in {

        val vatRates = List(vatRateAndSalesFromCountry)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithTotalAndVatQuery, vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getCountriesWithIncompleteSales()

          result mustBe empty
        }
      }

      "return a list of incomplete corrections if there are incomplete sales for one country" in {

        val vatRates = List(vatRateAndSalesFromCountryIncomplete)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithTotalAndVatQuery, vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getCountriesWithIncompleteSales()

          result mustBe List(country)
        }
      }

      "return a list of incomplete corrections if there are incomplete sales for more than one country" in {

        val secondCountry = arbitrary[Country].sample.value

        val vatRateAndSalesFromSecondCountryIncomplete: SalesToCountryWithOptionalSales =
          vatRateAndSalesFromCountryIncomplete.copy(
            country = secondCountry
          )

        val vatRates = List(vatRateAndSalesFromCountryIncomplete, vatRateAndSalesFromSecondCountryIncomplete)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithTotalAndVatQuery, vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.getCountriesWithIncompleteSales()

          result mustBe List(country, secondCountry)
        }
      }

    }

    "firstIndexedIncompleteCountrySales" - {

      "return None if there are no incomplete country sales" in {

        val noIncompleteCountries = Seq.empty

        val vatRates = List(vatRateAndSalesFromCountry)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithTotalAndVatQuery, vatRates).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCountrySales(noIncompleteCountries)

          result mustBe empty
        }
      }

      "return country and index if there are incomplete country sales" in {

        val incompleteCountries = Seq(country)

        val userAnswers = emptyUserAnswers
          .set(AllSalesWithTotalAndVatQuery, List(vatRateAndSalesFromCountryIncomplete))
          .success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.firstIndexedIncompleteCountrySales(incompleteCountries)

          result mustBe Some((vatRateAndSalesFromCountryIncomplete, 0))
        }
      }
    }

    "soldGoodsAnswered" - {

      "must return true if the SoldGoods page has been answered" in {

        val userAnswers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.soldGoodsAnswered

          result mustBe true
        }
      }

      "must return false if the SoldGoods page has not been answered" in {

        val userAnswers = emptyUserAnswers

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.soldGoodsAnswered

          result mustBe false
        }
      }
    }

    "correctPreviousReturnAnswered" - {

      "must return true if CorrectPreviousReturn page has been answered and there is more than one fulfilled obligation" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectPreviousReturnPage(0), true).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.correctPreviousReturnAnswered(numberOfFulfilledObligations = 2)

          result mustBe true
        }
      }

      "must return false if CorrectPreviousReturn page has been answered but there are no fulfilled obligations" in {

        val userAnswers = emptyUserAnswers
          .set(CorrectPreviousReturnPage(0), true).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.correctPreviousReturnAnswered(numberOfFulfilledObligations = 0)

          result mustBe false
        }
      }

      "must return false if CorrectPreviousReturn page has not been answered" in {

        val userAnswers = emptyUserAnswers

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.correctPreviousReturnAnswered(numberOfFulfilledObligations = 2)

          result mustBe false
        }
      }

    }

    "incompleteReturnsJourneyRedirect" - {

      "must redirect to the SoldGoods controller if it has not been answered" in {

        val userAnswers = emptyUserAnswers

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.incompleteReturnsJourneyRedirect(waypoints, numberOfFulfilledObligations = 2)

          result mustBe Some(Redirect(routes.SoldGoodsController.onPageLoad(waypoints)))
        }
      }

      "must redirect to the CorrectPreviousReturn controller if it has not been answered" in {

        val userAnswers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.incompleteReturnsJourneyRedirect(waypoints, numberOfFulfilledObligations = 2)

          result mustBe Some(Redirect(corrections.routes.CorrectPreviousReturnController.onPageLoad(waypoints)))
        }
      }

      "must return none when SoldGoods and CorrectPreviousReturn have been answered" in {

        val userAnswers = emptyUserAnswers
          .set(SoldGoodsPage, false).success.value
          .set(CorrectPreviousReturnPage(0), true).success.value

        val application = applicationBuilder(Some(userAnswers)).build()

        running(application) {
          implicit val request: DataRequest[AnyContent] = dataRequest(userAnswers)

          val result = TestCompletionChecks.incompleteReturnsJourneyRedirect(waypoints, numberOfFulfilledObligations = 2)

          result mustBe empty


        }
      }
    }
  }
}

