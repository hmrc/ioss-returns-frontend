package controllers.external

import base.SpecBase
import controllers.external.routes
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.NoMoreWelshView

class NoMoreWelshControllerSpec extends SpecBase {

  "NoMoreWelsh Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoMoreWelshController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoMoreWelshView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
