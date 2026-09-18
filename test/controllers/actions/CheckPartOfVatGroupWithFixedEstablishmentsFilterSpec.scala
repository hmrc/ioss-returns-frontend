package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import models.RegistrationWrapper
import models.etmp.VatCustomerInfo
import models.requests.RegistrationRequest
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import play.api.test.FakeRequest

import play.api.mvc.Results.Redirect
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CheckPartOfVatGroupWithFixedEstablishmentsFilterSpec extends SpecBase {

  private val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  when(mockFrontendAppConfig.registeredAsVatGroupUrl) thenReturn "/test-url"

  private val registrationWrapper: RegistrationWrapper = arbitraryRegistrationWrapper.arbitrary.sample.value

  private def makeRequest(registrationWrapper: RegistrationWrapper): RegistrationRequest[_] = {
    RegistrationRequest(FakeRequest(), testCredentials, Some(vrn), companyName, iossNumber, registrationWrapper, Some(intermediaryNumber), enrolments)
  }

  class Harness extends CheckPartOfVatGroupWithFixedEstablishmentsFilterImpl(mockFrontendAppConfig) {
    def callFilter(request: RegistrationRequest[_]): Future[Option[Result]] = filter(request)
  }

  "CheckPartOfVatGroupWithFixedEstablishmentsFilter" - {

    "must return None when registration is not part of vat group" in {

      val vatInfoNoneVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = false)
      val registrationWrapperNoneVatGroup: RegistrationWrapper = registrationWrapper.copy(vatInfo = Some(vatInfoNoneVatGroup))

      val request = makeRequest(registrationWrapperNoneVatGroup)
      val controller = new Harness

      val result = controller.callFilter(request).futureValue
      result `mustBe` None
    }

    "must return None when registration is part of vat group and has no Fixed Establishments" in {

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
    }

    "must redirect to URL when registration is part of vat group and has Fixed Establishments" in {

      val vatInfoVatGroup: VatCustomerInfo = registrationWrapper.vatInfo.head.copy(partOfVatGroup = true)
      val registrationWrapperVatGroupWithFE: RegistrationWrapper = registrationWrapper
        .copy(vatInfo = Some(vatInfoVatGroup))

      val request = makeRequest(registrationWrapperVatGroupWithFE)
      val controller = new Harness

      val result = controller.callFilter(request).futureValue
      result `mustBe` Some(Redirect(mockFrontendAppConfig.registeredAsVatGroupUrl))
    }
  }
}
