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

package services.intermediary

import base.SpecBase
import connectors.IntermediaryRegistrationConnector
import models.etmp.intermediary.{EtmpClientDetails, IntermediaryRegistrationWrapper}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

class IntermediaryClientServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockIntermediaryRegistrationConnector: IntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
  private val service = new IntermediaryClientService(mockIntermediaryRegistrationConnector)
  private val registration = arbitrary[IntermediaryRegistrationWrapper].sample.value

  implicit private val hc: HeaderCarrier = new HeaderCarrier()

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockIntermediaryRegistrationConnector
    )
  }

  ".getClientName" - {

    "must return the intermediary client name when a matching client exists" in {

      val intermediaryNumber = "IN9001234567"
      val iossNumber = "IM9001234567"
      val clientName = "Test Client"
      val intermediaryRegistration = registration.copy(
        etmpDisplayRegistration = registration.etmpDisplayRegistration.copy(
          clientDetails = Seq(
            arbitrary[EtmpClientDetails].sample.value.copy(
              clientIossID = iossNumber,
              clientName = clientName
            )
          )
        )
      )

      when(mockIntermediaryRegistrationConnector.get(intermediaryNumber)(hc)).thenReturn(Future.successful(intermediaryRegistration))

      val result = service.getClientName(
        isIntermediary = true,
        intermediaryNumber = Some(intermediaryNumber),
        iossNumber = iossNumber
      )

      result.futureValue mustBe clientName

      verify(mockIntermediaryRegistrationConnector)
        .get(intermediaryNumber)(hc)
    }

    "must return an empty string when the user is not an intermediary" in {

      val result = service.getClientName(
        isIntermediary = false,
        intermediaryNumber = None,
        iossNumber = "IM9001234567"
      )

      result.futureValue mustBe ""

      verifyNoInteractions(mockIntermediaryRegistrationConnector)
    }

    "must return an empty string when no matching client exists" in {

      val intermediaryNumber = "IN9001234567"
      val iossNumber = "IM9001234567"

      val intermediaryRegistration = registration.copy(
        etmpDisplayRegistration = registration.etmpDisplayRegistration.copy(
          clientDetails = Seq.empty
        )
      )

      when(mockIntermediaryRegistrationConnector.get(intermediaryNumber)(hc)).thenReturn(Future.successful(intermediaryRegistration))

      val result = service.getClientName(
        isIntermediary = true,
        intermediaryNumber = Some(intermediaryNumber),
        iossNumber = iossNumber
      )

      result.futureValue mustBe ""
    }

    "must fail when the user is an intermediary but has no intermediary number" in {

      val result = service.getClientName(
        isIntermediary = true,
        intermediaryNumber = None,
        iossNumber = "IM9001234567"
      )

      val exception = result.failed.futureValue

      exception mustBe a[RuntimeException]
      exception.getMessage mustBe "No intermediary number in request"

      verifyNoInteractions(mockIntermediaryRegistrationConnector)
    }

    "must propagate an error returned by the connector" in {

      val intermediaryNumber = "IN9001234567"
      val expectedException = new RuntimeException("Connector failed")

      when(
        mockIntermediaryRegistrationConnector.get(intermediaryNumber)(hc)
      ).thenReturn(Future.failed(expectedException))

      val result = service.getClientName(
        isIntermediary = true,
        intermediaryNumber = Some(intermediaryNumber),
        iossNumber = "IM9001234567"
      )

      result.failed.futureValue mustBe expectedException
    }
  }
}
