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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import java.net.URI
@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")
  val origin: String  = configuration.get[String]("origin")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "ioss-returns-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val loginUrl: String         = configuration.get[String]("urls.login")
  val loginContinueUrl: String = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String       = configuration.get[String]("urls.signOut")
  val amendRegistrationUrl: String = configuration.get[String]("urls.amendRegistrationUrl")
  val cancelYourRequestToLeaveUrl: String = configuration.get[String]("urls.cancelYourRequestToLeaveUrl")
  val leaveThisServiceUrl: String = configuration.get[String]("urls.leaveThisServiceUrl")
  val rejoinThisServiceUrl: String = configuration.get[String]("urls.rejoinThisServiceUrl")
  val paymentsBaseUrl: Service = configuration.get[Service]("microservice.services.pay-api")

  private val exitSurveyBaseUrl: String = configuration.get[String]("microservice.services.feedback-frontend.host") +
    configuration.get[String]("microservice.services.feedback-frontend.basePath")
  val exitSurveyUrl: String             = s"${exitSurveyBaseUrl}/${origin.toLowerCase}"

  val ivUpliftUrl: String = configuration.get[String]("urls.ivUplift")

  val allowedRedirectUrls: Seq[String] = configuration.get[Seq[String]]("urls.allowedRedirects")

  val ivEvidenceStatusUrl: String =
    s"${configuration.get[Service]("microservice.services.identity-verification").baseUrl}/disabled-evidences?origin=$origin"

  private val ivJourneyServiceUrl: String =
    s"${configuration.get[Service]("microservice.services.identity-verification").baseUrl}/journey/"

  def ivJourneyResultUrl(journeyId: String): String = new URI(s"$ivJourneyServiceUrl$journeyId").toString

  val languageTranslationEnabled: Boolean = configuration.get[Boolean]("features.welsh-translation")
  val exclusionsEnabled: Boolean = configuration.get[Boolean]("features.exclusions.enabled")

  val coreValidationService: Service = configuration.get[Service]("microservice.services.core-validation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Long]("mongodb.timeToLiveInSeconds")

  val saveForLaterTtl: Int = configuration.get[Int]("mongodb.saveForLaterTTLInDays")

  val iossEnrolment: String = configuration.get[String]("ioss-enrolment")
  val internalAuthToken: String = configuration.get[String]("internal-auth.token")
  val intermediaryEnabled: Boolean = configuration.get[Boolean]("features.intermediary.enabled")
  val intermediaryEnrolment: String = configuration.get[String]("features.intermediary.enrolment")

  val authLoginStubSignInUrl: String = configuration.get[String]("urls.authStubSignIn")

  lazy val userResearchUrl1: String = configuration.get[String]("urls.userResearch1")

  lazy val userResearchUrl2: String = configuration.get[String]("urls.userResearch2")
}
