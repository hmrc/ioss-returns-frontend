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

package services

import base.SpecBase
import config.FrontendAppConfig
import models.audit.JsonAuditModel
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  implicit val emptyHc: HeaderCarrier = HeaderCarrier()
  private val mockConfiguration = mock[FrontendAppConfig]
  private val mockAuditConnector: AuditConnector = mock[AuditConnector]
  private val auditService = new AuditService(mockConfiguration, mockAuditConnector)

  override protected def beforeEach(): Unit = {
    reset(mockAuditConnector)
    when(mockConfiguration.appName).thenReturn("ioss-exclusions-frontend")
  }

  "AuditService" - {

    "build and send an ExtendedDataEvent with the correct details" in {
      val mockAuditModel: JsonAuditModel = new JsonAuditModel {
        override val auditType: String = "TestAuditType"
        override val transactionName: String = "TestTransaction"
        override val detail: JsObject = Json.obj("key" -> "value")
      }
      implicit val request: Request[_] = FakeRequest("GET", "/test-path")

      auditService.audit(mockAuditModel)

      val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
      verify(mockAuditConnector).sendExtendedEvent(captor.capture())(any(), any())

      val capturedEvent = captor.getValue
      capturedEvent.auditSource mustBe "ioss-exclusions-frontend"
      capturedEvent.auditType mustBe mockAuditModel.auditType
      capturedEvent.tags must contain("transactionName" -> mockAuditModel.transactionName)
      capturedEvent.detail mustBe mockAuditModel.detail
    }
  }
}
