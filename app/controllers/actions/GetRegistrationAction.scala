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

import config.FrontendAppConfig
import connectors.{IntermediaryRegistrationConnector, RegistrationConnector}
import controllers.routes
import logging.Logging
import models.requests.{IdentifierRequest, RegistrationRequest}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import services.AccountService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRegistrationAction(
                             accountService: AccountService,
                             intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                             registrationConnector: RegistrationConnector,
                             config: FrontendAppConfig,
                             requestedIossNumber: String
                           )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, RegistrationRequest] with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
    
    (for {
      maybeIossNumberFromEnrolments <- findIossFromEnrolments(request.enrolments)
      maybeIntermediaryNumber <- findIntermediaryFromEnrolments(request.enrolments)
    } yield {
      (maybeIossNumberFromEnrolments, maybeIntermediaryNumber) match {
        case (Some(iossNumberFromEnrolments), _) if iossNumberFromEnrolments == requestedIossNumber =>
          getIossRegistrationAndMakeRequest(iossNumberFromEnrolments, request)

        case (_, Some(intermediaryNumber)) =>
          checkIntermediaryAccessAndFormRequest(intermediaryNumber, requestedIossNumber, request)

        case _ =>
          Left(Redirect(routes.NotRegisteredController.onPageLoad())).toFuture
      }
    }).flatten
  }

  private def getIossRegistrationAndMakeRequest[A](iossNumber: String, request: IdentifierRequest[A])(implicit hc: HeaderCarrier) = {
    registrationConnector.get(iossNumber).map { registration =>
      Right(RegistrationRequest(
        request.request,
        request.credentials,
        Some(request.vrn),
        registration.getCompanyName(),
        iossNumber,
        registration,
        None,
        request.enrolments
      ))
    }
  }

  private def checkIntermediaryAccessAndFormRequest[A](intermediaryNumber: String, iossNumber: String, request: IdentifierRequest[A])
                                                      (implicit hc: HeaderCarrier) = {

    def buildRegistrationRequest(intermediaryNumber: String): Future[Either[Result, RegistrationRequest[A]]] = {
      registrationConnector.get(iossNumber).map { registrationWrapper =>
        Right(RegistrationRequest(
          request = request.request,
          credentials = request.credentials,
          vrn = registrationWrapper.maybeVrn,
          companyName = registrationWrapper.getCompanyName(),
          iossNumber = iossNumber,
          registrationWrapper = registrationWrapper,
          intermediaryNumber = Some(intermediaryNumber),
          enrolments = request.enrolments
        ))
      }
    }

    intermediaryRegistrationConnector.get(intermediaryNumber).flatMap { currentRegistration =>

      val hasDirectAccess = currentRegistration.etmpDisplayRegistration.clientDetails.map(_.clientIossID).contains(iossNumber)

      if (hasDirectAccess) {
        buildRegistrationRequest(intermediaryNumber)
      } else {
        val allIntermediaryEnrolments = findIntermediariesFromEnrolments(request.enrolments)

        findAuthorisedIntermediaryForIossClient(allIntermediaryEnrolments, iossNumber).flatMap {
          case Some(authorisedIntermediaryNumber) =>
            buildRegistrationRequest(authorisedIntermediaryNumber)
          case None =>
            logger.warn(
              s"Intermediary $intermediaryNumber tried to access iossNumber $iossNumber, but they aren't the intermediary of this ioss number"
            )
            Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad())).toFuture
        }
      }
    }
  }

  private def findAuthorisedIntermediaryForIossClient(intermediaryNumbers: Seq[String], iossNumber: String)
                                                     (implicit hc: HeaderCarrier): Future[Option[String]] = {

    def isAuthorisedToAccessIossClient(intermediaryNumber: String): Future[Boolean] = {
      intermediaryRegistrationConnector.get(intermediaryNumber).map { registration =>
        registration.etmpDisplayRegistration.clientDetails.map(_.clientIossID).contains(iossNumber)
      }
    }

    Future.sequence(intermediaryNumbers.map { intermediaryNumber =>
        isAuthorisedToAccessIossClient(intermediaryNumber)
          .map(isAuthorised => intermediaryNumber -> isAuthorised)
      })
      .map(_.collectFirst { case (intermediaryNumber, true) => intermediaryNumber })
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

  private def findIntermediaryFromEnrolments(enrolments: Enrolments): Future[Option[String]] = {
    enrolments
      .enrolments
      .filter(_.key == config.intermediaryEnrolment)
      .flatMap(_.identifiers.filter(_.key == "IntNumber").map(_.value))
      .headOption
      .toFuture
  }

  private def findIntermediariesFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-INT")
      .flatMap(_.identifiers.find(id => id.key == "IntNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }
}

class GetRegistrationActionProvider @Inject(
                                             accountService: AccountService,
                                             intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                             registrationConnector: RegistrationConnector,
                                             config: FrontendAppConfig
                                           )(implicit val executionContext: ExecutionContext) {
  def apply(requestedIossNumber: String): GetRegistrationAction = {
    new GetRegistrationAction(
      accountService,
      intermediaryRegistrationConnector,
      registrationConnector,
      config,
      requestedIossNumber
    )
  }
}