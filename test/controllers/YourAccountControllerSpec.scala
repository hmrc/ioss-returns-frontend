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
import generators.Generators
import models.payments.{Payment, PaymentStatus, PrepareData}
import models.{Period, RegistrationWrapper}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PaymentsService
import play.api.inject.bind
import java.time.{LocalDate, Month}
import scala.concurrent.Future

class YourAccountControllerSpec extends SpecBase with MockitoSugar with Generators with BeforeAndAfterEach {

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

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), registration = registrationWrapper)
          .overrides(bind[PaymentsService].to(paymentsService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.YourAccountController.onPageLoad(waypoints).url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }
    }

  }
}