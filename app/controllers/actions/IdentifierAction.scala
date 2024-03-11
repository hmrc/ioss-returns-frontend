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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import logging.Logging
import models.requests.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import services.AccountService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}


class IdentifierAction @Inject()(
                                  override val authConnector: AuthConnector,
                                  accountService: AccountService,
                                  config: FrontendAppConfig
                                )
                                (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[Request, IdentifierRequest]
    with AuthorisedFunctions with Logging {

  //noinspection ScalaStyle
  override def refine[A](request: Request[A]): Future[Either[Result, IdentifierRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(
      AuthProviders(AuthProvider.GovernmentGateway) and
        (AffinityGroup.Individual or AffinityGroup.Organisation) and
        CredentialStrength(CredentialStrength.strong)
    ).retrieve( Retrievals.credentials and
      Retrievals.allEnrolments and
      Retrievals.affinityGroup and
      Retrievals.confidenceLevel and
      Retrievals.credentialRole ) {

      case Some(credentials) ~ enrolments ~ Some(Organisation) ~ _ ~ Some(credentialRole) if credentialRole == User =>
        findIossFromEnrolments(enrolments).map { maybeIossNumber =>
          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(iossNumber)) =>
              getSuccessfulResponse(request, credentials, vrn, iossNumber)
            case _ => throw InsufficientEnrolments()
          }
        }


      case _ ~ _ ~ Some(Organisation) ~ _ ~ Some(credentialRole) if credentialRole == Assistant =>
        throw UnsupportedCredentialRole()

      case Some(credentials) ~ enrolments ~ Some(Individual) ~ confidence ~ _ =>
        findIossFromEnrolments(enrolments).map { maybeIossNumber =>
          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(iossNumber)) =>
              checkConfidenceAndGetResponse(request, credentials, vrn, iossNumber, confidence)
            case _ =>
              throw InsufficientEnrolments()
          }
        }

      case _ =>
        throw new UnauthorizedException("Unable to retrieve authorisation data")

    } recoverWith {
      case _: NoActiveSession =>
        Left(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))).toFuture
      case _: AuthorisationException =>
        Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
    }
  }

  private def getSuccessfulResponse[A](
                                        request: Request[A],
                                        credentials: Credentials,
                                        vrn: Vrn,
                                        iossNumber: String
                                      ): Either[Result, IdentifierRequest[A]] = {
    val identifierRequest = IdentifierRequest(request, credentials, vrn, iossNumber)
    Right(identifierRequest)
  }

  private def checkConfidenceAndGetResponse[A](
                                                request: Request[A],
                                                credentials: Credentials,
                                                vrn: Vrn,
                                                iossNumber: String,
                                                confidence: ConfidenceLevel
                                              ): Either[Result, IdentifierRequest[A]] = {
    if (confidence >= ConfidenceLevel.L200) {
      getSuccessfulResponse(request, credentials, vrn, iossNumber)
    } else {
      throw InsufficientConfidenceLevel()
    }
  }

  private def findIossFromEnrolments(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val filteredIossNumbers = enrolments.enrolments.filter(_.key == config.iossEnrolment).flatMap(_.identifiers.filter(_.key == "IOSSNumber").map(_.value)).toSeq

    filteredIossNumbers match {
      case firstEnrolment :: Nil => Some(firstEnrolment).toFuture
      case multipleEnrolments if multipleEnrolments.nonEmpty =>
        accountService.getLatestAccount().map(x => Some(x))
      case _ => None.toFuture
    }
  }

  private def findVrnFromEnrolments(enrolments: Enrolments): Option[Vrn] =
    enrolments.enrolments.find(_.key == "HMRC-MTD-VAT")
      .flatMap { enrolment => enrolment.identifiers.find(_.key == "VRN").map(e => Vrn(e.value))
      } orElse enrolments.enrolments.find(_.key == "HMCE-VATDEC-ORG")
      .flatMap { enrolment => enrolment.identifiers.find(_.key == "VATRegNo").map(e => Vrn(e.value)) }

}
