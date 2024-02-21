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

package services

import base.SpecBase
import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import utils.FutureSyntax.FutureOps

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class AccountServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private implicit val emptyHc: HeaderCarrier = HeaderCarrier()

  private val mockRegistrationConnector = mock[RegistrationConnector]
  private val mockFrontendAppConfig = mock[FrontendAppConfig]

  override protected def beforeEach(): Unit = {
    reset(mockRegistrationConnector)
  }

  "getAccounts" - {

    "return the most recent iossNumber" in {
      val enrolmentKey = "HMRC-IOSS-ORG"

      val enrolments = EACDEnrolments(Seq(
        EACDEnrolment(
          service = "service",
          state = "state",
          activationDate = Some(LocalDateTime.of(2023, 7, 3, 10, 30)),
          identifiers = Seq(EACDIdentifiers(enrolmentKey, "IM9009876543"))
        ),
        EACDEnrolment(
          service = "service",
          state = "state",
          activationDate = Some(LocalDateTime.of(2023, 6, 1, 10, 30)),
          identifiers = Seq(EACDIdentifiers(enrolmentKey, "IM9005555555"))
        )
      ))

      when(mockRegistrationConnector.getAccounts()(any())) thenReturn enrolments.toFuture
      when(mockFrontendAppConfig.iossEnrolment) thenReturn enrolmentKey
      val service = new AccountService(mockFrontendAppConfig, mockRegistrationConnector)

      val result = service.getLatestAccount().futureValue

      result mustBe "IM9009876543"
    }

  }
}
