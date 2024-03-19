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

package controllers

import base.SpecBase
import forms.StartReturnFormProvider
import models.PartialReturnPeriod
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.{NoOtherPeriodsAvailablePage, SoldGoodsPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PartialReturnPeriodService
import views.html.StartReturnView

import java.time.Month
import scala.concurrent.Future

class StartReturnControllerSpec extends SpecBase with MockitoSugar with ScalaCheckPropertyChecks {

  val formProvider = new StartReturnFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val startReturnRoute: String = routes.StartReturnController.onPageLoad(waypoints, period).url

  val extraNumberOfDays: Int = 5

  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  "StartReturn Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, None, isFinalReturn = false, None)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when partial return" in {
      val partialReturn = Some(PartialReturnPeriod(period.firstDay, period.lastDay, period.year, Month.DECEMBER))

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(partialReturn)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(form, waypoints, period, None, isFinalReturn = false, partialReturn)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe SoldGoodsPage.route(waypoints).url
      }
    }

    "must redirect to the No Other Periods Available page when answer is no" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe NoOtherPeriodsAvailablePage.route(waypoints).url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {

        val request =
          FakeRequest(POST, startReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[StartReturnView]

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe view(boundForm, waypoints, period, None, isFinalReturn = false, None)(request, messages(application)).toString
      }
    }

    "must redirect to Excluded Not Permitted when a trader is excluded and the period's last day is after their exclusion effective date" in {

      val effectiveDate = Gen.choose(
        period.lastDay.minusDays(1 + extraNumberOfDays),
        period.lastDay.minusDays(1)
      ).sample.value

      val noLongerSuppliesExclusion = EtmpExclusion(
        NoLongerSupplies,
        effectiveDate,
        effectiveDate,
        quarantine = false
      )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(noLongerSuppliesExclusion)))
      ).build()

      running(application) {
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ExcludedNotPermittedController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET when a trader is excluded and the period's last day is before their exclusion effective date" in {
      when(mockPartialReturnPeriodService.getPartialReturnPeriod(any(), any())(any())) thenReturn Future.successful(None)

      val effectiveDate = Gen.choose(
        period.lastDay,
        period.lastDay.plusDays(extraNumberOfDays)
      ).sample.value

      val noLongerSuppliesExclusion = EtmpExclusion(
        NoLongerSupplies,
        effectiveDate,
        effectiveDate,
        quarantine = false
      )

      val application = applicationBuilder(
        userAnswers = Some(emptyUserAnswers),
        registration = registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(noLongerSuppliesExclusion)))
      ) .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
        .build()

      running(application) {
        val request = FakeRequest(GET, startReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[StartReturnView]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          form,
          waypoints,
          period,
          Some(noLongerSuppliesExclusion),
          isFinalReturn = false,
          None
        )(request, messages(application)).toString
      }
    }
  }
}
