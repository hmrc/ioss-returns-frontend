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

package controllers.previousReturns

import base.SpecBase
import models.Period
import models.etmp.EtmpObligationDetails
import models.payments.{Payment, PrepareData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{ObligationsService, PaymentsService}
import utils.FutureSyntax.FutureOps
import views.html.previousReturns.SubmittedReturnsHistoryView

class SubmittedReturnsHistoryControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val obligationDetails: Seq[EtmpObligationDetails] =
    Gen.listOfN(5, arbitraryObligationDetails.arbitrary).sample.value

  private val obligationPeriods: Seq[Period] = obligationDetails.map(_.periodKey).map(Period.fromKey)

  private val payments: List[Payment] =
    obligationPeriods.map { period =>
      arbitraryPayment.arbitrary.sample.value.copy(period = period)
    }.toList

  private val prepareData: PrepareData = {
    PrepareData(
      duePayments = List(payments.head),
      overduePayments = payments.tail,
      totalAmountOwed = payments.map(_.amountOwed).sum,
      totalAmountOverdue = BigDecimal(0),
      iossNumber = iossNumber
    )
  }

  private val emptyPrepareData: PrepareData = {
    PrepareData(
      duePayments = List.empty,
      overduePayments = List.empty,
      totalAmountOwed = BigDecimal(0),
      totalAmountOverdue = BigDecimal(0),
      iossNumber = iossNumber
    )
  }

  private val mockPaymentsService: PaymentsService = mock[PaymentsService]
  private val mockObligationsService: ObligationsService = mock[ObligationsService]

  override def beforeEach(): Unit = {
    Mockito.reset(mockPaymentsService)
    Mockito.reset(mockObligationsService)
  }

  "SubmittedReturnsHistory Controller" - {

    "must return OK and the correct view for a GET when there are submitted returns" in {

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = obligationPeriods.flatMap { period =>
        Map(period -> payments.filter(_.period == period).head)
      }.groupBy(_._1.year)


      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn obligationDetails.toFuture
        when(mockPaymentsService.prepareFinancialData()(any(), any())) thenReturn prepareData.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(waypoints, periodsWithFinancialData)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when there are no submitted returns" in {

      val periodsWithFinancialData: Map[Int, Seq[(Period, Payment)]] = Map.empty

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[ObligationsService].toInstance(mockObligationsService))
        .overrides(bind[PaymentsService].toInstance(mockPaymentsService))
        .build()

      running(application) {

        when(mockObligationsService.getFulfilledObligations(any())(any())) thenReturn Seq.empty.toFuture
        when(mockPaymentsService.prepareFinancialData()(any(), any())) thenReturn emptyPrepareData.toFuture

        val request = FakeRequest(GET, routes.SubmittedReturnsHistoryController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmittedReturnsHistoryView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(waypoints, periodsWithFinancialData)(request, messages(application)).toString
      }
    }

    // TODO api down test? use vatReturn instead?

  }
}
