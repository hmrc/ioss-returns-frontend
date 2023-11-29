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
import forms.CheckSalesFormProvider
import models.{Country, UserAnswers, VatOnSales, VatRateFromCountry}
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.{CheckSalesPage, JourneyRecoveryPage, SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.VatRateService
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, Card, CardTitle}
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.{CheckSalesSummary, SalesToCountrySummary, VatOnSalesSummary}
import views.html.CheckSalesView
import viewmodels.govuk.SummaryListFluency

class CheckSalesControllerSpec extends SpecBase with MockitoSugar with SummaryListFluency with BeforeAndAfterEach {

  private val country: Country = arbitraryCountry.arbitrary.sample.value
  private val mockVatRateService: VatRateService = mock[VatRateService]

  private val formProvider = new CheckSalesFormProvider()
  private val form: Form[Boolean] = formProvider()

  private val vatRateFromCountry: VatRateFromCountry = arbitraryVatRateFromCountry.arbitrary.sample.value
  private val salesValue: Int = 1234

  private val baseAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), country).success.value
    .set(VatRatesFromCountryPage(index), List[VatRateFromCountry](vatRateFromCountry)).success.value
    .set(SalesToCountryPage(index, index), salesValue).success.value
    .set(VatOnSalesPage(index, index), VatOnSales.values.head).success.value

  private lazy val checkSalesRoute: String = CheckSalesPage(Some(index)).route(waypoints).url
//  private lazy val DeleteVatRateSalesForCountryRoute: String = CheckSalesPage(Some(index)).route(waypoints).url

  override def beforeEach(): Unit = {
    reset(mockVatRateService)
    super.beforeEach()
  }

  "CheckSales Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockVatRateService.countRemainingVatRatesForCountry(index, baseAnswers)) thenReturn Seq(vatRateFromCountry)

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[VatRateService].toInstance(mockVatRateService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)

        val request = FakeRequest(GET, checkSalesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckSalesView]

        // TODO
//        val list = Seq(
//          SummaryListViewModel(
////            CheckSalesSummary.rows(baseAnswers, waypoints, index)
//            rows = Seq(
//              SalesToCountrySummary.row(baseAnswers, waypoints, index, index, CheckSalesPage()),
//              VatOnSalesSummary.row(baseAnswers, waypoints, index, index, CheckSalesPage())
//            ).flatten
//          ).withCard(Card(
//            title = Some(CardTitle(content = HtmlContent(messages(application)))),
//            actions = Some(Actions(
//              items = Seq(
//                ActionItemViewModel(HtmlContent(messages(application)), DeleteVatRateSalesForCountryRoute)
//                  .withVisuallyHiddenText(messages(application)))
//            )))
//        ))

        val list = Seq.empty

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(form, waypoints, period, list, index, country, canAddAnotherVatRate = true)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = emptyUserAnswers.set(CheckSalesPage(), true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkSalesRoute)

        val view = application.injector.instanceOf[CheckSalesView]

        val result = route(application, request).value

        // TODO
        val list = List.empty

        status(result) mustBe OK
        contentAsString(result) mustBe
          view(form.fill(true), waypoints, period, list, index, country, canAddAnotherVatRate = false)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn true.toFuture

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, checkSalesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe CheckSalesPage().navigate(waypoints, emptyUserAnswers, emptyUserAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, checkSalesRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CheckSalesView]

        val result = route(application, request).value

        // TODO
        val list = List.empty

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe
          view(boundForm, waypoints, period, list, index, country, canAddAnotherVatRate = true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, checkSalesRoute)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, checkSalesRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe JourneyRecoveryPage.route(waypoints).url
      }
    }
  }
}
