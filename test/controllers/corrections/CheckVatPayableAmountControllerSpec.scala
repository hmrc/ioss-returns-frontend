/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.corrections

import base.SpecBase
import config.Constants.periodYear
import models.Country
import org.scalacheck.Arbitrary.arbitrary
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CorrectionReturnYearPage, VatAmountCorrectionCountryPage}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.corrections.{PreviouslyDeclaredCorrectionAmount, PreviouslyDeclaredCorrectionAmountQuery}

class CheckVatPayableAmountControllerSpec extends SpecBase {


  private lazy val CheckVatAmountRoute: String = routes.CheckVatPayableAmountController.onPageLoad(waypoints, index, index).url
  private lazy val CheckVatAmountPostRoute: String = routes.CheckVatPayableAmountController.onSubmit(waypoints, index, index, incompletePromptShown = false).url
  private val country = arbitrary[Country].sample.value
  private val baseAnswers = emptyUserAnswers
    .set(CorrectionCountryPage(index, index), country).success.value
    .set(CorrectionReturnYearPage(index), periodYear).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(100.0)).success.value

  private val answersNoVat = emptyUserAnswers
    .set(CorrectionCountryPage(index, index), country).success.value
    .set(CorrectionReturnYearPage(index), periodYear).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value

  "CheckVatPayableAmount Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = baseAnswers.set(
        PreviouslyDeclaredCorrectionAmountQuery(index, index),
        PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = BigDecimal(0))
      ).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, CheckVatAmountRoute)

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result).contains("Correction amount") mustBe true
        contentAsString(result).contains("Previous VAT total declared") mustBe true
        contentAsString(result).contains("New VAT total") mustBe true
      }

    }

    "must return OK and the correct view with missing data warning for a GET" in {

      val userAnswers = answersNoVat.set(
        PreviouslyDeclaredCorrectionAmountQuery(index, index),
        PreviouslyDeclaredCorrectionAmount(previouslyDeclared = false, amount = BigDecimal(0))
      ).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, CheckVatAmountRoute)

        val result = route(application, request).value

        status(result) mustBe OK
        contentAsString(result).contains("Correction amount") mustBe false
        contentAsString(result).contains("Previous VAT total declared") mustBe true
        contentAsString(result).contains("New VAT total") mustBe false
        contentAsString(result).contains("Some of your information is missing. You must complete this before you can submit your changes.") mustBe true
      }
    }

    "must redirect to Journey Recovery if no correction period or country found in user answers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, CheckVatAmountRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to CorrectionListCountries on POST" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .build()

      running(application) {
        val request = FakeRequest(POST, CheckVatAmountPostRoute)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, index).url
      }
    }
  }
}
