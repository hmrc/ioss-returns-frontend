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

package models.requests

import models.{RegistrationWrapper, UserAnswers}
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.domain.Vrn

case class OptionalDataRequest[A] (
                                    request: Request[A],
                                    credentials: Credentials,
                                    vrn: Option[Vrn],
                                    iossNumber: String,
                                    companyName: String,
                                    registrationWrapper: RegistrationWrapper,
                                    intermediaryNumber: Option[String],
                                    userAnswers: Option[UserAnswers]
                                  ) extends WrappedRequest[A](request) {

  val userId: String = credentials.providerId
  val isIntermediary: Boolean = intermediaryNumber.nonEmpty
  
}

case class DataRequest[A] (
                            request: Request[A],
                            credentials: Credentials,
                            vrn: Option[Vrn],
                            iossNumber: String,
                            companyName: String,
                            registrationWrapper: RegistrationWrapper,
                            intermediaryNumber: Option[String],
                            userAnswers: UserAnswers
                          ) extends WrappedRequest[A](request) {

  val userId: String = credentials.providerId
  val isIntermediary: Boolean = intermediaryNumber.nonEmpty

  lazy val vrnOrError: Vrn = vrn.getOrElse(
    throw new IllegalStateException("VRN required and not found")
  )
}