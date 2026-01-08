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

package services

import base.SpecBase
import connectors.{IntermediaryRegistrationConnector, VatReturnConnector}
import models.{Country, TotalVatToCountry}
import models.core.*
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.CREATED
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.http.HttpResponse

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CoreVatReturnServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockReturnConnector = mock[VatReturnConnector]
  private val mockSalesAtVatRateService = mock[SalesAtVatRateService]
  private val mockIntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]

  implicit private lazy val request: DataRequest[AnyContent] = DataRequest(
    FakeRequest(),
    Credentials("providerID", "providerTYPE"),
    Some(vrn),
    iossNumber,
    companyName,
    registrationWrapper,
    None,
    completeUserAnswers
  )

  override protected def beforeEach(): Unit = {
    reset(mockReturnConnector)
    reset(mockSalesAtVatRateService)
  }

  "CoreVatReturnService" - {
    "must correctly convert a vat return and submit it returning still owed amount" in {

      val total = BigDecimal(734964.25)

      when(mockReturnConnector.submit(any(), any())(any())) thenReturn Future.successful(HttpResponse(CREATED, "Vat Return created"))
      when(mockSalesAtVatRateService.getTotalVatOwedAfterCorrections(any())) thenReturn total
      when(mockSalesAtVatRateService.getVatOwedToCountries(any())) thenReturn List(TotalVatToCountry(
        Country("HR", "Croatia"),
        BigDecimal(20)
      ))

      val service = new CoreVatReturnService(mockReturnConnector, mockSalesAtVatRateService, mockIntermediaryRegistrationConnector, stubClockAtArbitraryDate)

      val expectedVatReturnReference = s"XI/$iossNumber/M0${period.month.getValue}.${period.year}"
      val expectedCoreVatReturn = CoreVatReturn(
        vatReturnReferenceNumber = expectedVatReturnReference,
        version = Instant.now(stubClockAtArbitraryDate),
        traderId = CoreTraderId(iossNumber, "XI", None),
        period = CorePeriod(period.year, s"0${period.month.getValue}"),
        startDate = period.firstDay,
        endDate = period.lastDay,
        submissionDateTime = Instant.now(stubClockAtArbitraryDate),
        totalAmountVatDueGBP = total,
        msconSupplies = List(
          CoreMsconSupply(
            msconCountryCode = "HR",
            balanceOfVatDueGBP = BigDecimal(20),
            grandTotalMsidGoodsGBP = BigDecimal(20),
            correctionsTotalGBP = BigDecimal(0),
            msidSupplies = List(CoreSupply(
              "GOODS",
              BigDecimal(20),
              "STANDARD",
              BigDecimal(100),
              BigDecimal(20)
            )),
            corrections = List.empty
          )
        ),
        changeDate = registrationWrapper.registration.adminUse.changeDate
      )

      service.submitCoreVatReturn(completeUserAnswers).futureValue mustBe total
      verify(mockReturnConnector, times(1)).submit(eqTo(expectedCoreVatReturn), eqTo(iossNumber))(any())

    }
  }

}
