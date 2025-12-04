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

import config.FrontendAppConfig
import connectors.{IntermediaryRegistrationConnector, RegistrationConnector}
import logging.Logging
import models.requests.{IdentifierRequest, RegistrationRequest}
import play.api.mvc.{ActionRefiner, Result}
import play.api.mvc.Results.Redirect
import repositories.IntermediarySelectedIossNumberRepository
import services.AccountService
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.FutureSyntax.FutureOps

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GetRegistrationAction @Inject()(
                                       val registrationConnector: RegistrationConnector
                                     )(implicit val executionContext: ExecutionContext)
  extends ActionRefiner[IdentifierRequest, RegistrationRequest] {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request.request, request.request.session)
    findIossFromEnrolments(request.enrolments).flatMap {
      case Some(iossNumber) =>
        registrationConnector.get(iossNumber).map { registration =>
          Right(RegistrationRequest(request.request, request.credentials, request.vrn, iossNumber, registration, None, request.enrolments))
        }
      case None if isIntermediary(request.enrolments) =>
        findIntermediaryFromEnrolments(request.enrolments).flatMap { maybeIntermediaryNumber =>
          (maybeIntermediaryNumber, requestedMaybeIossNumber) match {
            case (Some(intermediaryNumber), Some(iossNumber)) =>
              checkIntermediaryAccessAndFormRequest(intermediaryNumber, iossNumber, request)
            case (Some(intermediaryNumber), None) =>
              intermediarySelectedIossNumberRepository.get(request.userId).flatMap {
                case Some(intermediarySelectedIossNumber) =>
                  intermediarySelectedIossNumberRepository.keepAlive(request.userId).flatMap { _ =>
                    checkIntermediaryAccessAndFormRequest(intermediaryNumber, intermediarySelectedIossNumber.iossNumber, request)
                  }
                case _ =>
                  logger.warn(
                    s"Intermediary $maybeIntermediaryNumber did not have a request iossNumber, nor one found in selector repository"
                  )
                  Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad())).toFuture
              }
            case _ =>
              logger.warn(
                s"expected intermediary and ioss number didn't get one: $maybeIntermediaryNumber $requestedMaybeIossNumber"
              )
              Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad())).toFuture
          }
        }
      case _ =>
        Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad())).toFuture
    }
  }

  private def checkIntermediaryAccessAndFormRequest(intermediaryNumber: String, iossNumber: String, request: IdentifierRequest[_])
                                                   (implicit hc: HeaderCarrier) = {
    intermediaryRegistrationConnector.get(intermediaryNumber).flatMap { intermediaryRegistrationWrapper =>
      val availableIossNumbers = intermediaryRegistrationWrapper.etmpDisplayRegistration.clientDetails.map(_.clientIossID)
      if (availableIossNumbers.contains(iossNumber)) {
        registrationConnector.get(iossNumber).map { registrationWrapper =>
          Right(RegistrationRequest(
            request.request,
            request.credentials,
            request.vrn,
            iossNumber,
            registrationWrapper,
            Some(intermediaryNumber),
            request.enrolments
          ))
        }
      } else {
        logger.warn(
          s"Intermediary $intermediaryNumber tried to access iossNumber $iossNumber, but they aren't the intermediary of this ioss number"
        )
        Left(Redirect(controllers.routes.NotRegisteredController.onPageLoad())).toFuture
      }
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

  private def findIntermediaryFromEnrolments(enrolments: Enrolments)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    // TODO multiple intermediary enrolments
    enrolments
      .enrolments
      .filter(_.key == config.intermediaryEnrolment)
      .flatMap(_.identifiers.filter(_.key == "IntNumber").map(_.value))
      .headOption
      .toFuture
  }

  private def isIntermediary(enrolments: Enrolments): Boolean = {
    enrolments
      .enrolments
      .exists(_.key == config.intermediaryEnrolment)
  }
}

class GetRegistrationActionProvider @Inject(
                                             accountService: AccountService,
                                             intermediaryRegistrationConnector: IntermediaryRegistrationConnector,
                                             registrationConnector: RegistrationConnector,
                                             intermediarySelectedIossNumberRepository: IntermediarySelectedIossNumberRepository,
                                             config: FrontendAppConfig
                                           )(implicit val executionContext: ExecutionContext) {
  def apply(maybeIossNumber: Option[String] = None): GetRegistrationAction =
    new GetRegistrationAction(
      accountService,
      intermediaryRegistrationConnector,
      registrationConnector,
      config,
      maybeIossNumber,
      intermediarySelectedIossNumberRepository
    )
}