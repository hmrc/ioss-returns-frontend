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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import logging.Logging
import models.requests.IdentifierRequest
import play.api.mvc.Results._
import play.api.mvc._
import services.{AccountService, UrlBuilderService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}


class IdentifierAction @Inject()(
                                  override val authConnector: AuthConnector,
                                  accountService: AccountService,
                                  config: FrontendAppConfig,
                                  urlBuilderService: UrlBuilderService,
                                )
                                (implicit val executionContext: ExecutionContext)
  extends ActionRefiner[Request, IdentifierRequest]
    with AuthorisedFunctions with Logging {

  private type IdentifierActionResult[A] = Future[Either[Result, IdentifierRequest[A]]]

  //noinspection ScalaStyle
  override def refine[A](request: Request[A]): IdentifierActionResult[A] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(
      (AffinityGroup.Individual or AffinityGroup.Organisation) and
        CredentialStrength(CredentialStrength.strong)
    ).retrieve(Retrievals.credentials and
      Retrievals.allEnrolments and
      Retrievals.affinityGroup and
      Retrievals.confidenceLevel
    ) {

      case Some(credentials) ~ enrolments ~ Some(Organisation) ~ _ =>
        findIossFromEnrolments(enrolments).map { maybeIossNumber =>
          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(iossNumber)) =>
              getSuccessfulResponse(request, credentials, vrn, iossNumber, enrolments)
            case _ => throw InsufficientEnrolments()
          }
        }

      case Some(credentials) ~ enrolments ~ Some(Individual) ~ confidence =>
        findIossFromEnrolments(enrolments).map { maybeIossNumber =>
          (findVrnFromEnrolments(enrolments), maybeIossNumber) match {
            case (Some(vrn), Some(iossNumber)) =>
              checkConfidenceAndGetResponse(request, credentials, vrn, iossNumber, confidence, enrolments)
            case _ =>
              throw InsufficientEnrolments()
          }
        }

      case _ =>
        throw new UnauthorizedException("Unable to retrieve authorisation data")

    } recoverWith {
      case _: NoActiveSession =>
        Left(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))).toFuture
      case _: InsufficientConfidenceLevel =>
        logger.info("Insufficient confidence level")
        upliftConfidenceLevel(request)
      case e: AuthorisationException =>
        logger.info(s"Got authorisation exception ${e.getMessage}", e)
        Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
    }
  }

  private def getSuccessfulResponse[A](
                                        request: Request[A],
                                        credentials: Credentials,
                                        vrn: Vrn,
                                        iossNumber: String,
                                        enrolments: Enrolments
                                      ): Either[Result, IdentifierRequest[A]] = {
    val identifierRequest = IdentifierRequest(request, credentials, vrn, iossNumber, enrolments)
    Right(identifierRequest)
  }

  private def checkConfidenceAndGetResponse[A](
                                                request: Request[A],
                                                credentials: Credentials,
                                                vrn: Vrn,
                                                iossNumber: String,
                                                confidence: ConfidenceLevel,
                                                enrolments: Enrolments
                                              ): Either[Result, IdentifierRequest[A]] = {
    if (confidence >= ConfidenceLevel.L250) {
      getSuccessfulResponse(request, credentials, vrn, iossNumber, enrolments)
    } else {
      throw InsufficientConfidenceLevel()
    }
  }

  private def findIossFromEnrolments(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val filteredIossNumbers = enrolments
      .enrolments
      .filter(_.key == config.iossEnrolment)
      .flatMap(_.identifiers.filter(_.key == "IOSSNumber").map(_.value))
      .toSeq

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

  private def upliftConfidenceLevel[A](request: Request[A]): IdentifierActionResult[A] = {
    val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(config.allowedRedirectUrls: _*)
    Left(Redirect(
      config.ivUpliftUrl,
      Map(
        "origin" -> Seq(config.origin),
        "confidenceLevel" -> Seq(ConfidenceLevel.L250.toString),
        "completionURL" -> Seq(urlBuilderService.loginContinueUrl(request).get(redirectPolicy).url),
        "failureURL" -> Seq(urlBuilderService.ivFailureUrl(request))
      )
    )).toFuture
  }
}
