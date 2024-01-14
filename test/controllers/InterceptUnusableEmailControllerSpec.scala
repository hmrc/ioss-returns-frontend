package controllers

import base.SpecBase
import config.FrontendAppConfig
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.InterceptUnusableEmailView

class InterceptUnusableEmailControllerSpec extends SpecBase {

  "InterceptUnusablEmail Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.InterceptUnusableEmailController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[InterceptUnusableEmailView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(registrationWrapper.registration.schemeDetails.businessEmailId, appConfig.amendRegistrationUrl)(request, messages(application)).toString
      }
    }
  }
}
