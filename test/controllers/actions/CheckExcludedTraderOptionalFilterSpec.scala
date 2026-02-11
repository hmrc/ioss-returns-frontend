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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.Period
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.requests.OptionalDataRequest
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import services.ExcludedTraderService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckExcludedTraderOptionalFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  class Harness(startReturnPeriod: Period) extends CheckExcludedTraderOptionalFilterImpl(startReturnPeriod, mockConfig, new ExcludedTraderService) {
    def callFilter(request: OptionalDataRequest[_]): Future[Option[Result]] = filter(request)
  }

  ".filter" - {

    "must return None when trader is not excluded" in {

      val application = applicationBuilder(None).build()

      when(mockConfig.exclusionsEnabled) thenReturn true

      running(application) {
        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, registrationWrapper, None, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must return None when trader is excluded but can complete returns up to and including their exclusion effective date" in {

      val application = applicationBuilder(None).build()

      when(mockConfig.exclusionsEnabled) thenReturn true

      running(application) {
        val effectiveDate: LocalDate = period.lastDay
        val excludedRegistration = registrationWrapper.copy(
          registration = registrationWrapper.registration.copy(
            exclusions = Seq(
              EtmpExclusion(
                NoLongerSupplies,
                effectiveDate,
                effectiveDate,
                false
              )
            )
          )
        )

        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, excludedRegistration, None, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }
    }

    "must Redirect when trader is excluded" in {

      val application = applicationBuilder(None).build()

      when(mockConfig.exclusionsEnabled) thenReturn true

      running(application) {
        val effectiveDate: LocalDate = period.firstDay.minusDays(1)
        val excludedRegistration = registrationWrapper.copy(
          registration = registrationWrapper.registration.copy(
            exclusions = Seq(
              EtmpExclusion(
                NoLongerSupplies,
                effectiveDate,
                effectiveDate,
                false
              )
            )
          )
        )

        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, excludedRegistration, None, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result.value mustBe Redirect(routes.ExcludedNotPermittedController.onPageLoad())
      }

    }

    "must return None when trader is excluded but exclusions are disabled" in {

      val application = applicationBuilder(None).build()

      when(mockConfig.exclusionsEnabled) thenReturn false

      running(application) {
        val effectiveDate: LocalDate = period.lastDay.minusDays(1)
        val excludedRegistration = registrationWrapper.copy(
          registration = registrationWrapper.registration.copy(
            exclusions = Seq(
              EtmpExclusion(
                NoLongerSupplies,
                effectiveDate,
                effectiveDate,
                false
              )
            )
          )
        )

        val request = OptionalDataRequest(FakeRequest(), testCredentials, Some(vrn), iossNumber, companyName, excludedRegistration, None, Some(completeUserAnswers))
        val controller = new Harness(period)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }

    }
  }

}
