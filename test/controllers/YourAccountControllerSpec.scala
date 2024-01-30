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
import config.FrontendAppConfig
import connectors.ReturnStatusConnector
import generators.Generators
import models.SubmissionStatus._
import models.{Period, RegistrationWrapper, SubmissionStatus}
import models.etmp.EtmpExclusion
import models.etmp.EtmpExclusionReason.NoLongerSupplies
import models.payments.{Payment, PaymentStatus, PrepareData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentsService
import viewmodels.PaymentsViewModel
import viewmodels.yourAccount.{CurrentReturns, Return, ReturnsViewModel}
import views.html.YourAccountView

import java.time.{LocalDate, Month}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

  val nextPeriod: Period = Period(LocalDate.now.minusYears(1).getYear, Month.APRIL)

  private val returnStatusConnector = mock[ReturnStatusConnector]

  "Your Account Controller" - {

    "should display your account view" - {
      "when registration wrapper is present" in {
        val now = LocalDate.now()
        val periodOverdue1 = Period(now.minusYears(1).getYear, Month.JANUARY)
        val periodOverdue2 = Period(now.minusYears(1).getYear, Month.FEBRUARY)
        val periodDue1 = Period(now.getYear, now.getMonth.plus(1))
        val periodDue2 = Period(now.getYear, now.getMonth.plus(2))
        val paymentOverdue1 = Payment(periodOverdue1, 10, periodOverdue1.paymentDeadline, PaymentStatus.Unpaid)
        val paymentOverdue2 = Payment(periodOverdue2, 10, periodOverdue2.paymentDeadline, PaymentStatus.Unpaid)
        val paymentDue1 = Payment(periodDue1, 10, periodDue1.paymentDeadline, PaymentStatus.Unpaid)
        val paymentDue2 = Payment(periodDue2, 10, periodDue2.paymentDeadline, PaymentStatus.Unpaid)
        val prepareData = PrepareData(List(paymentDue1, paymentDue2),
          List(paymentOverdue1, paymentOverdue2),
          List(paymentDue1,
            paymentDue2,
            paymentOverdue1,
            paymentOverdue2
          ).map(_.amountOwed).sum,
          List(paymentOverdue1, paymentOverdue2).map(_.amountOwed).sum,
          iossNumber)
        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value
        val paymentsService = mock[PaymentsService]
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(prepareData)

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))
            ))
          )

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapper)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].to(paymentsService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }

    "must return OK with leaveThisService link and without cancelYourRequestToLeave link when a trader is not excluded" in {
      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(
          Right(CurrentReturns(
            Seq(Return(
              nextPeriod,
              nextPeriod.firstDay,
              nextPeriod.lastDay,
              nextPeriod.paymentDeadline,
              SubmissionStatus.Next,
              inProgress = false,
              isOldest = false
            ))
          ))
        )

      val paymentsService = mock[PaymentsService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .overrides(
          bind[ReturnStatusConnector].toInstance(returnStatusConnector),
          bind[PaymentsService].toInstance(paymentsService)
        )
        .build()


      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
      when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
        Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          Some(appConfig.leaveThisServiceUrl),
          None,
          ReturnsViewModel(
            Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            )
          )(messages(application))
        )(request, messages(application)).toString
      }
    }

    "must return OK with cancelYourRequestToLeave link and without leaveThisService link when a trader is excluded" in {
      val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

      val exclusion = EtmpExclusion(
        NoLongerSupplies,
        LocalDate.now(stubClockAtArbitraryDate).plusDays(2),
        LocalDate.now(stubClockAtArbitraryDate).minusDays(1),
        false
      )
      val registrationWrapperEmptyExclusions: RegistrationWrapper =
        registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq(exclusion)))

      val paymentsService = mock[PaymentsService]

      when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
        Future.successful(
          Right(CurrentReturns(
            Seq(Return(
              nextPeriod,
              nextPeriod.firstDay,
              nextPeriod.lastDay,
              nextPeriod.paymentDeadline,
              SubmissionStatus.Next,
              inProgress = false,
              isOldest = false
            ))
          ))
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
        .overrides(
          bind[ReturnStatusConnector].toInstance(returnStatusConnector),
          bind[PaymentsService].toInstance(paymentsService)
        )
        .build()

      val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
      when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
        Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

      running(application) {

        val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourAccountView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe OK
        contentAsString(result) mustBe view(
          registrationWrapper.vatInfo.getName,
          iossNumber,
          paymentsViewModel,
          appConfig.amendRegistrationUrl,
          None,
          Some(appConfig.cancelYourRequestToLeaveUrl),
          ReturnsViewModel(
            Seq(
              Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
            )
          )(messages(application))
        )(request, messages(application)).toString
      }
    }

    "must return OK and the correct view with no saved answers" - {

      "when there are no returns due" in {
        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return(
                nextPeriod,
                nextPeriod.firstDay,
                nextPeriod.lastDay,
                nextPeriod.paymentDeadline,
                SubmissionStatus.Next,
                inProgress = false,
                isOldest = false
              ))
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(nextPeriod, Next, inProgress = false, isOldest = false)
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return due" in {
        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(Return.fromPeriod(period, Due, inProgress = false, isOldest = false
              ))
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(period, Due, inProgress = false, isOldest = false)
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

      "and 1 return overdue" in {

        val firstPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.JANUARY)
        val secondPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.FEBRUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
              )
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(secondPeriod, Due, inProgress = false, isOldest = false),
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true)
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

      "when there is 1 return overdue" in {

        val period = Period(LocalDate.now.minusYears(1).getYear, Month.JANUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return.fromPeriod(period, Overdue, inProgress = false, isOldest = true),
              )
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(period, Overdue, inProgress = false, isOldest = true)
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

      "when there is 2 returns overdue" in {
        val firstPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.JANUARY)
        val secondPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.FEBRUARY)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false)
              )
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

      "when there is multiple returns overdue and one due" in {
        val firstPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.JANUARY)
        val secondPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.FEBRUARY)
        val thirdPeriod = Period(LocalDate.now.minusYears(1).getYear, Month.MARCH)

        val registrationWrapper: RegistrationWrapper = arbitrary[RegistrationWrapper].sample.value

        val registrationWrapperEmptyExclusions: RegistrationWrapper =
          registrationWrapper.copy(registration = registrationWrapper.registration.copy(exclusions = Seq.empty))

        when(returnStatusConnector.getCurrentReturns(any())(any())) thenReturn
          Future.successful(
            Right(CurrentReturns(
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
                Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
              )
            ))
          )

        val paymentsService = mock[PaymentsService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapperEmptyExclusions)
          .overrides(
            bind[ReturnStatusConnector].toInstance(returnStatusConnector),
            bind[PaymentsService].toInstance(paymentsService)
          )
          .build()


        val paymentsViewModel = PaymentsViewModel(Seq.empty, Seq.empty)(messages(application))
        when(paymentsService.prepareFinancialData()(any(), any())) thenReturn
          Future.successful(PrepareData(List.empty, List.empty, 0, 0, iossNumber))

        running(application) {

          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[YourAccountView]
          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustBe OK
          contentAsString(result) mustBe view(
            registrationWrapper.vatInfo.getName,
            iossNumber,
            paymentsViewModel,
            appConfig.amendRegistrationUrl,
            Some(appConfig.leaveThisServiceUrl),
            None,
            ReturnsViewModel(
              Seq(
                Return.fromPeriod(firstPeriod, Overdue, inProgress = false, isOldest = true),
                Return.fromPeriod(secondPeriod, Overdue, inProgress = false, isOldest = false),
                Return.fromPeriod(thirdPeriod, Due, inProgress = false, isOldest = false)
              )
            )(messages(application))
          )(request, messages(application)).toString
        }
      }

    }
  }
}