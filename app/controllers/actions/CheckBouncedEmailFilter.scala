/*
 * Copyright 2023 HM Revenue & Customs
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

import models.requests.RegistrationRequest
import play.api.mvc.{ActionFilter, Result}
import play.api.mvc.Results.Redirect

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckBouncedEmailFilterImpl()(implicit val executionContext: ExecutionContext)
  extends ActionFilter[RegistrationRequest] {

  override protected def filter[A](request: RegistrationRequest[A]): Future[Option[Result]] = {

    if (request.registrationWrapper.registration.schemeDetails.unusableStatus) {
      Future(Some(Redirect(controllers.routes.InterceptUnusableEmailController.onPageLoad())))
    } else {
      Future(None)
    }

  }
}

class CheckBouncedEmailFilterProvider @Inject()()(implicit ec: ExecutionContext) {

  def apply(): CheckBouncedEmailFilterImpl =
    new CheckBouncedEmailFilterImpl()

}