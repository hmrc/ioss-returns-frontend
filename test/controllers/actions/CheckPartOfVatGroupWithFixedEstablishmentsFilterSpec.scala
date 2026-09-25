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
import connectors.IntermediaryRegistrationConnector
import models.RegistrationWrapper
import models.etmp.VatCustomerInfo
import models.etmp.intermediary.{EtmpClientDetails, IntermediaryRegistrationWrapper}
import models.requests.RegistrationRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import utils.FutureSyntax.FutureOps

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckPartOfVatGroupWithFixedEstablishmentsFilterSpec extends SpecBase with BeforeAndAfterEach {

  private val mockIntermediaryRegistrationConnector: IntermediaryRegistrationConnector = mock[IntermediaryRegistrationConnector]
  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value
  private val intermediaryRegistration = arbitraryIntermediaryRegistrationWrapper.arbitrary.sample.value

  private def makeRequest(registrationWrapper: RegistrationWrapper, intermediaryNumber: Option[String] = None): RegistrationRequest[_] = {
    RegistrationRequest(FakeRequest(), testCredentials, Some(vrn), companyName, iossNumber, registrationWrapper, intermediaryNumber, enrolments)
  }

  class Harness extends CheckPartOfVatGroupWithFixedEstablishmentsFilterImpl(mockIntermediaryRegistrationConnector, mockFrontendAppConfig) {
    def callFilter(request: RegistrationRequest[_]): Future[Option[Result]] = filter(request)
  }

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockFrontendAppConfig,
      mockIntermediaryRegistrationConnector
    )
  }

  "CheckPartOfVatGroupWithFixedEstablishmentsFilter" - {

    "when an Intermediary doesn't exist" - {

      "must return None when registration is not part of vat group with Fixed Establishments" in {

        val vatInfoNoneVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = false)
        val registrationWrapperNoneVatGroup: RegistrationWrapper = registrationWrapper.copy(vatInfo = Some(vatInfoNoneVatGroup))

        val request = makeRequest(registrationWrapperNoneVatGroup)
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` None
        verifyNoInteractions(mockIntermediaryRegistrationConnector)
      }

      "must return None when registration is part of vat group without Fixed Establishments" in {

        val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
        val registrationWrapperVatGroupWithoutFE: RegistrationWrapper = registrationWrapper
          .copy(
            vatInfo = Some(vatInfoVatGroup),
            registration = registrationWrapper.registration
              .copy(schemeDetails = registrationWrapper.registration.schemeDetails
                .copy(euRegistrationDetails = Seq.empty))
          )

        val request = makeRequest(registrationWrapperVatGroupWithoutFE)
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` None
        verifyNoInteractions(mockIntermediaryRegistrationConnector)
      }

      "must redirect to URL when registration is part of vat group with Fixed Establishments" in {

        when(mockFrontendAppConfig.amendRegistrationUrl) thenReturn "/test-url"

        val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
        val registrationWrapperVatGroupWithFE: RegistrationWrapper = registrationWrapper
          .copy(vatInfo = Some(vatInfoVatGroup))

        val application = applicationBuilder().build()

        running(application) {

          val request = makeRequest(registrationWrapperVatGroupWithFE)
          val controller = new Harness

          val result = controller.callFilter(request).futureValue
          result `mustBe` Some(Redirect("/test-url"))
          verifyNoInteractions(mockIntermediaryRegistrationConnector)
          verify(mockFrontendAppConfig, times(1)).amendRegistrationUrl
        }
      }
    }

    "when an Intermediary exists" - {

      "must return None when registration is not part of vat group with fixed establishments and is not a client of the intermediary" in {

        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn intermediaryRegistration.toFuture

        val vatInfoNoneVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = false)
        val registrationWrapperNoneVatGroup: RegistrationWrapper = registrationWrapper.copy(vatInfo = Some(vatInfoNoneVatGroup))

        val request = makeRequest(registrationWrapperNoneVatGroup, Some(intermediaryNumber))
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` None
        verify(mockIntermediaryRegistrationConnector, times(1)).get(eqTo(intermediaryNumber))(any())
      }

      "must redirect to URL when registration is part of vat group with fixed establishments and is not a client of the intermediary" in {

        when(mockFrontendAppConfig.amendRegistrationUrl) thenReturn "/test-url"
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn intermediaryRegistration.toFuture

        val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
        val registrationWrapperNoneVatGroup: RegistrationWrapper = registrationWrapper.copy(vatInfo = Some(vatInfoVatGroup))

        val request = makeRequest(registrationWrapperNoneVatGroup, Some(intermediaryNumber))
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` Some(Redirect("/test-url"))
        verify(mockIntermediaryRegistrationConnector, times(1)).get(eqTo(intermediaryNumber))(any())
        verify(mockFrontendAppConfig, times(1)).amendRegistrationUrl
      }

      "must return None when registration is part of vat group without fixed establishments and is not a client of the intermediary" in {

        when(mockFrontendAppConfig.amendRegistrationUrl) thenReturn "/test-url"
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn intermediaryRegistration.toFuture

        val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
        val registrationWrapperVatGroupWithoutFE: RegistrationWrapper = registrationWrapper
          .copy(
            vatInfo = Some(vatInfoVatGroup),
            registration = registrationWrapper.registration.copy(
              schemeDetails = registrationWrapper.registration.schemeDetails.copy(
                euRegistrationDetails = Seq.empty
              )
            )
          )

        val request = makeRequest(registrationWrapperVatGroupWithoutFE, Some(intermediaryNumber))
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` None
        verify(mockIntermediaryRegistrationConnector, times(1)).get(eqTo(intermediaryNumber))(any())
        verifyNoInteractions(mockFrontendAppConfig)
      }

      "must return None when registration is a client of the intermediary" in {

        val clientIntermediaryRegistration: IntermediaryRegistrationWrapper = intermediaryRegistration.copy(
          etmpDisplayRegistration = intermediaryRegistration.etmpDisplayRegistration.copy(
            clientDetails = intermediaryRegistration.etmpDisplayRegistration.clientDetails :+
              intermediaryRegistration.etmpDisplayRegistration.clientDetails.head.copy(
                clientIossID = iossNumber
              )
          )
        )

        when(mockFrontendAppConfig.amendRegistrationUrl) thenReturn "/test-url"
        when(mockIntermediaryRegistrationConnector.get(any())(any())) thenReturn clientIntermediaryRegistration.toFuture

        val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
        val registrationWrapperVatGroupWithoutFE: RegistrationWrapper = registrationWrapper
          .copy(
            vatInfo = Some(vatInfoVatGroup),
            registration = registrationWrapper.registration.copy(
              schemeDetails = registrationWrapper.registration.schemeDetails.copy(
                euRegistrationDetails = Seq.empty
              )
            )
          )

        val request = makeRequest(registrationWrapperVatGroupWithoutFE, Some(intermediaryNumber))
        val controller = new Harness

        val result = controller.callFilter(request).futureValue
        result `mustBe` None
        verify(mockIntermediaryRegistrationConnector, times(1)).get(eqTo(intermediaryNumber))(any())
        verifyNoInteractions(mockFrontendAppConfig)
      }
    }
  }
}
