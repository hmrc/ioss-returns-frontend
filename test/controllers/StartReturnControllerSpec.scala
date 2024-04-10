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
import connectors.ReturnStatusConnector
import forms.StartReturnFormProvider
import models.SubmissionStatus.{Complete, Due, Excluded, Next, Overdue}
import models.{PartialReturnPeriod, StandardPeriod, SubmissionStatus}
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, IdiomaticMockito, Mockito}
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.{EmptyWaypoints, NoOtherPeriodsAvailablePage, SoldGoodsPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PartialReturnPeriodService
import viewmodels.yourAccount.{CurrentReturns, Return}
import views.html.StartReturnView

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class StartReturnControllerSpec
  extends SpecBase
    with IdiomaticMockito
    with ScalaCheckPropertyChecks
    with TableDrivenPropertyChecks
    with BeforeAndAfterEach {

  val formProvider = new StartReturnFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val startReturnRoute: String = routes.StartReturnController.onPageLoad(waypoints, period).url

  private val mockReturnStatusConnector: ReturnStatusConnector = mock[ReturnStatusConnector]
  private val mockPartialReturnPeriodService = mock[PartialReturnPeriodService]

  private val maybePartialReturn = Some(PartialReturnPeriod(period.firstDay, period.lastDay, period.year, Month.DECEMBER))

  val extraNumberOfDays: Int = 5

  private val emptyCurrentReturns = CurrentReturns(
    returns = List.empty,
    finalReturnsCompleted = false,
    completeOrExcludedReturns = List.empty
  )

  override protected def beforeEach(): Unit = {
    resetMocks()
  }

  private def resetMocks() : Unit = {
    Mockito.reset(mockReturnStatusConnector)
    Mockito.reset(mockPartialReturnPeriodService)
  }

  "StartReturn Controller" - {

    "GET" - {
      "must redirect when there are no returns" in {
        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(Right(emptyCurrentReturns)))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .build()

        running(application) {
          val request = FakeRequest(GET, startReturnRoute)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.CannotStartReturnController.onPageLoad().url
        }
      }

      "must return OK and a successful view for a GET when the return is DUE or Overdue without a partial return" in {
        val options = Table(
          "status",
          Due,
          Overdue,
        )

        forAll(options) { submissionStatus =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))))

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
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
      }

      "must return OK and a successful view for a GET when the return is DUE or Overdue with a partial return" in {
        val options = Table(
          "status",
          Due,
          Overdue,
        )

        forAll(options) { submissionStatus =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))))

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(maybePartialReturn))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
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
              None,
              isFinalReturn = false,
              maybePartialReturn
            )(request, messages(application)).toString
          }
        }
      }


      "must redirect when the status is Complete or Excluded" in {
        val options = Table(
          ("status", "redirect call"),
          (Complete, () => controllers.previousReturns.routes.SubmittedReturnForPeriodController.onPageLoad(EmptyWaypoints, period).url),
          (Excluded, () => routes.CannotStartExcludedReturnController.onPageLoad().url)
        )

        forAll(options) { (submissionStatus, redirectCall) =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(completeOrExcludedReturns = List(createReturn(submissionStatus, period))))
            ))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request = FakeRequest(GET, startReturnRoute)
            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustEqual redirectCall()
          }
        }
      }

      "must redirect when the status is Next as it is not valid for the current period" in {
        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(
            Right(emptyCurrentReturns.copy(returns = List(createReturn(Next, period))))
          ))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .build()

        running(application) {
          val request = FakeRequest(GET, startReturnRoute)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustEqual routes.NoOtherPeriodsAvailableController.onPageLoad().url
        }
      }

      "must return OK and the correct view for a GET when a trader is excluded and the period's last day is before their exclusion effective date" in {
        val effectiveDate = period.lastDay.plusDays(extraNumberOfDays)

        val noLongerSuppliesExclusion = EtmpExclusion(
          exclusionReason = NoLongerSupplies,
          effectiveDate = effectiveDate,
          decisionDate = effectiveDate,
          quarantine = false
        )

        val registrationWrapperWithExclusion =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(noLongerSuppliesExclusion)))

        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(
            Right(emptyCurrentReturns.copy(returns = List(createReturn(Due, period))))
          ))

        when(mockPartialReturnPeriodService.getPartialReturnPeriod(
          ArgumentMatchers.eq(registrationWrapperWithExclusion),
          ArgumentMatchers.eq(period)
        )(any()))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          registration = registrationWrapperWithExclusion
        ).overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
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

    def createReturn(submissionStatus: SubmissionStatus, period: StandardPeriod = StandardPeriod(1970, Month.JANUARY)): Return = {
      Return(
        period = period,
        firstDay = LocalDate.EPOCH,
        lastDay = LocalDate.EPOCH,
        dueDate = LocalDate.EPOCH,
        submissionStatus = submissionStatus,
        inProgress = false,
        isOldest = false
      )
    }

    "POST" - {
      "must redirect to the next page when a YES is submitted for a current due or overdue period" in {
        val options = Table(
          "status",
          Due,
          Overdue
        )

        forAll(options) { submissionStatus =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))
            ))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
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
      }

      "must redirect to the No Other Periods Available page when answer is no" in {
        val options = Table(
          "status",
          Due,
          Overdue
        )

        forAll(options) { submissionStatus =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))
            ))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
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
      }

      "must redirect when the return is marked as Next as it is not current" in {
        when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
          .thenReturn(Future.successful(
            Right(emptyCurrentReturns.copy(returns = List(createReturn(Next, period))))
          ))

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
          .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, startReturnRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe NoOtherPeriodsAvailablePage.route(waypoints).url
        }
      }


      "must redirect when the return is marked as complete or excluded as it is not current" in {
        val options = Table(
          ("status", "redirect call"),
          (Complete, () => controllers.previousReturns.routes.SubmittedReturnForPeriodController.onPageLoad(EmptyWaypoints, period).url),
          (Excluded, () => routes.CannotStartExcludedReturnController.onPageLoad().url)
        )

        forAll(options) { (submissionStatus, redirectCall) =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(completeOrExcludedReturns = List(createReturn(submissionStatus, period))))
            ))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request =
              FakeRequest(POST, startReturnRoute)
                .withFormUrlEncodedBody(("value", "true"))

            val result = route(application, request).value

            status(result) mustBe SEE_OTHER
            redirectLocation(result).value mustBe redirectCall()
          }
        }
      }


      "must return a Bad Request and errors when invalid data is submitted" in {
        val options = Table(
          "status",
          Due,
          Overdue
        )

        forAll(options) { submissionStatus =>
          resetMocks()

          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))
            ))

          when(mockPartialReturnPeriodService.getPartialReturnPeriod(
            ArgumentMatchers.eq(registrationWrapper),
            ArgumentMatchers.eq(period)
          )(any()))
            .thenReturn(Future.successful(None))

          val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
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
      }

      "must redirect to Excluded Not Permitted when a trader is excluded and the period's last day is after their exclusion effective date" in {
        val options = Table(
          "status",
          Due,
          Overdue
        )

        forAll(options) { submissionStatus =>
          when(mockReturnStatusConnector.getCurrentReturns(ArgumentMatchers.eq(iossNumber))(any()))
            .thenReturn(Future.successful(
              Right(emptyCurrentReturns.copy(returns = List(createReturn(submissionStatus, period))))
            ))

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
          )
            .overrides(bind[ReturnStatusConnector].toInstance(mockReturnStatusConnector))
            .overrides(bind[PartialReturnPeriodService].toInstance(mockPartialReturnPeriodService))
            .build()

          running(application) {
            val request = FakeRequest(GET, startReturnRoute)

            val result = route(application, request).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual routes.ExcludedNotPermittedController.onPageLoad().url
          }
        }
      }
    }
  }
}
