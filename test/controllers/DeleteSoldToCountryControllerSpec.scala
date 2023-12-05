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

package controllers

import base.SpecBase
import forms.DeleteSoldToCountryFormProvider
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.{DeleteSoldToCountryPage, JourneyRecoveryPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import queries.SalesByCountryQuery
import repositories.SessionRepository
import utils.FutureSyntax.FutureOps
import views.html.DeleteSoldToCountryView

class DeleteSoldToCountryControllerSpec extends SpecBase with MockitoSugar {
  
  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: BigDecimal = 1234
  
  private val formProvider = new DeleteSoldToCountryFormProvider()
  private val form: Form[Boolean] = formProvider(country)

  private val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index), List(vatRateFromCountry)).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index), VatOnSales.values.head).success.value

  private lazy val deleteSoldToCountryRoute: String = routes.DeleteSoldToCountryController.onPageLoad(waypoints, index).url

  "DeleteSoldToCountry Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteSoldToCountryRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteSoldToCountryView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, index, country)(request, messages(application)).toString
      }
    }
    
    "must delete a record and then redirect to the next page when the user answers Yes" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteSoldToCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = baseAnswers.remove(SalesByCountryQuery(index)).success.value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteSoldToCountryPage(index).navigate(waypoints, baseAnswers, expectedAnswers).url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedAnswers))
      }
    }
    
    "must not delete a record and then redirect to the next page when the user answers No" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture
      
      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteSoldToCountryRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe DeleteSoldToCountryPage(index).navigate(waypoints, baseAnswers, baseAnswers).url
        verifyNoInteractions(mockSessionRepository)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteSoldToCountryRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteSoldToCountryView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, index, country)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteSoldToCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
    
    "must redirect to Journey Recovery for a GET if the sales are not found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteSoldToCountryRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteSoldToCountryRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
