/*
 * Copyright 2025 HM Revenue & Customs
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
import controllers.routes
import forms.corrections.CorrectionListCountriesFormProvider
import models.Country
import models.etmp.{EtmpObligationDetails, EtmpObligationsFulfilmentStatus}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections._
import play.api.data.Form
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import services.ObligationsService
import utils.FutureSyntax.FutureOps
import viewmodels.checkAnswers.corrections.CorrectionListCountriesSummary
import viewmodels.govuk.SummaryListFluency
import views.html.corrections.CorrectionListCountriesView

import scala.concurrent.Future

class CorrectionListCountriesControllerSpec extends SpecBase with SummaryListFluency with MockitoSugar {

  val formProvider = new CorrectionListCountriesFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val correctionListCountriesRoute: String = controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, index).url
  lazy val correctionListCountriesRoutePost: String =
    controllers.corrections.routes.CorrectionListCountriesController.onSubmit(waypoints, index, incompletePromptShown = false).url

  private val country = arbitrary[Country].sample.value
  private val obligationService: ObligationsService = mock[ObligationsService]
  private val etmpObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AL"
    )
  )

  private val baseAnswers = emptyUserAnswers
    .set(CorrectionCountryPage(index, index), country).success.value
    .set(CorrectionReturnYearPage(index), 2023).success.value
    .set(CorrectionReturnPeriodPage(index), period).success.value
    .set(VatAmountCorrectionCountryPage(index, index), BigDecimal(100.0)).success.value

  private val answersWithNoCorrectionValue =
    emptyUserAnswers
      .set(CorrectionCountryPage(index, index), country).success.value
      .set(CorrectionReturnYearPage(index), 2023).success.value
      .set(CorrectionReturnPeriodPage(index), period).success.value

  "CorrectionListCountries Controller" - {

    "must return OK and the correct view for a GET" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionListCountriesView]
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          waypoints,
          list,
          period,
          period,
          index,
          canAddCountries = true,
          incompleteCountries = List.empty,
          isIntermediary = false, companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val view = application.injector.instanceOf[CorrectionListCountriesView]

        val result = route(application, request).value
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          waypoints,
          list,
          period,
          period,
          index,
          canAddCountries = true,
          incompleteCountries = List.empty,
          isIntermediary = false, companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view with missing data warning for a GET" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(answersWithNoCorrectionValue))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CorrectionListCountriesView]
        val list = CorrectionListCountriesSummary.addToListRows(answersWithNoCorrectionValue, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          waypoints,
          list,
          period,
          period,
          index,
          canAddCountries = true,
          incompleteCountries = List(country.name),
          isIntermediary = false, companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseAnswers))
          .overrides(bind[ObligationsService].toInstance(obligationService))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request = FakeRequest(POST, correctionListCountriesRoutePost).withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value
        val expectedAnswers = emptyUserAnswers.set(CorrectionListCountriesPage(index), true).success.value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual CorrectionListCountriesPage(index, Some(index)).navigate(waypoints, expectedAnswers, expectedAnswers).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(obligationService.getFulfilledObligations(any())(any())) thenReturn etmpObligationDetails.toFuture

      val application = applicationBuilder(userAnswers = Some(baseAnswers))
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        implicit val msgs: Messages = messages(application)
        val request =
        FakeRequest(POST, correctionListCountriesRoutePost)
          .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[CorrectionListCountriesView]

        val result = route(application, request).value
        val list = CorrectionListCountriesSummary.addToListRows(baseAnswers, waypoints, index, CorrectionListCountriesPage(index))

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          waypoints,
          list,
          period,
          period,
          index,
          canAddCountries = true,
          incompleteCountries = List.empty,
          isIntermediary = false, companyName = "Company Name"
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[ObligationsService].toInstance(obligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, correctionListCountriesRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionListCountriesRoutePost)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must refresh if there is incomplete data and the prompt has not been shown before" in {

      val application = applicationBuilder(Some(answersWithNoCorrectionValue)).build()

      running(application) {
        val request =
          FakeRequest(POST, correctionListCountriesRoutePost)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, index).url
      }
    }

    "must redirect to VatAmountCorrectionCountry if there is incomplete data and the prompt has been shown before" in {

      val application = applicationBuilder(userAnswers = Some(answersWithNoCorrectionValue)).build()

      running(application) {
        val routePost = controllers.corrections.routes.CorrectionListCountriesController.onSubmit(waypoints, index, incompletePromptShown = true).url
        val request =
          FakeRequest(POST, routePost)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.VatAmountCorrectionCountryController.onPageLoad(waypoints, index, index).url
      }
    }
  }


}
