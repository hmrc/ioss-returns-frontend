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

package controllers.actions

import base.SpecBase
import connectors.VatReturnConnector
import models.etmp.{EtmpObligation, EtmpObligationDetails, EtmpObligations, EtmpObligationsFulfilmentStatus}
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class CheckSubmittedReturnsFilterSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  class Harness(connector: VatReturnConnector) extends CheckSubmittedReturnsFilterImpl(connector) {
    def callFilter(request: DataRequest[_]): Future[Option[Result]] = filter(request)
  }

  private val mockConnector = mock[VatReturnConnector]

  private val etmpObligations = arbitraryObligations.arbitrary.sample.value


  private val openObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Open,
      periodKey = "23AL"
    )
  )

  private val fulfilleddObligationDetails: Seq[EtmpObligationDetails] = Seq(
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Fulfilled,
      periodKey = "23AK"
    ),
    EtmpObligationDetails(
      status = EtmpObligationsFulfilmentStatus.Open,
      periodKey = "23AL"
    ),

  )

  private val openEtmpObligation = EtmpObligations(obligations = Seq(EtmpObligation(openObligationDetails)))

  private val fulfilledEtmpObligation = EtmpObligations(obligations = Seq(EtmpObligation(fulfilleddObligationDetails)))

  override def beforeEach(): Unit = {
    Mockito.reset(mockConnector)
  }

  ".filter" - {

    "must return None when submitted returns are found" in {

      when(mockConnector.getObligations(any())(any())) thenReturn Future.successful(fulfilledEtmpObligation)

      val application = applicationBuilder(None)
        .overrides(bind[VatReturnConnector].toInstance(mockConnector))
        .build()

      running(application) {
        val request = DataRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, completeUserAnswers)
        val controller = new Harness(mockConnector)

        val result = controller.callFilter(request).futureValue

        result must not be defined
      }

    }

    "must redirect to CheckYourAnswers page" - {

      "when no returns are found" in {

        when(mockConnector.getObligations(any())(any())) thenReturn Future.successful(etmpObligations.copy(obligations = Seq.empty))

        val application = applicationBuilder(None)
          .overrides(bind[VatReturnConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = DataRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, completeUserAnswers)
          val controller = new Harness(mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints))
        }
      }

      "when returns are found but there is no complete return" in {

        when(mockConnector.getObligations(any())(any())) thenReturn Future.successful(openEtmpObligation)

        val application = applicationBuilder(None)
          .overrides(bind[VatReturnConnector].toInstance(mockConnector))
          .build()

        running(application) {
          val request = DataRequest(FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, completeUserAnswers)
          val controller = new Harness(mockConnector)

          val result = controller.callFilter(request).futureValue

          result.value mustEqual Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints))
        }
      }
    }
  }
}
