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

import models.requests.{DataRequest, IdentifierRequest, OptionalDataRequest, RegistrationRequest}
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc._
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.ExecutionContext

trait AuthenticatedControllerComponents extends MessagesControllerComponents {

  def actionBuilder: DefaultActionBuilder

  def sessionRepository: SessionRepository

  def identify: IdentifierAction

  def getRegistration: GetRegistrationAction

  def getData: DataRetrievalActionProvider

  def requireData: DataRequiredAction

  def checkBouncedEmail: CheckBouncedEmailFilterProvider

  def withSavedAnswers: SavedAnswersRetrievalActionProvider

  def requirePreviousReturns: CheckSubmittedReturnsFilterProvider

  def checkExcludedTrader: CheckExcludedTraderFilter

  def checkExcludedTraderOptional: CheckExcludedTraderOptionalFilter

  def checkCommencementDate: CheckCommencementDateFilter

  def checkCommencementDateOptional: CheckCommencementDateOptionalFilter

  def checkIsCurrentReturnPeriodFilter: CheckIsCurrentReturnPeriodFilter

  def auth: ActionBuilder[IdentifierRequest, AnyContent] =
    actionBuilder andThen identify

  def authAndGetRegistrationWithoutCheckBouncedEmail: ActionBuilder[RegistrationRequest, AnyContent] = {
    auth andThen
      getRegistration
  }

  def authAndGetRegistration: ActionBuilder[RegistrationRequest, AnyContent] = {
    authAndGetRegistrationWithoutCheckBouncedEmail andThen
      checkBouncedEmail()
  }

  def authAndGetOptionalData(): ActionBuilder[OptionalDataRequest, AnyContent] = {
    authAndGetRegistration andThen getData()
  }

  def authAndRequireData(): ActionBuilder[DataRequest, AnyContent] = {
    authAndGetOptionalData() andThen requireData andThen checkExcludedTrader() andThen checkCommencementDate()
  }

  def authAndGetDataAndCorrectionEligible(): ActionBuilder[DataRequest, AnyContent] = {
    authAndRequireData() andThen
      requirePreviousReturns()
  }

}

case class DefaultAuthenticatedControllerComponents @Inject()(
                                                               messagesActionBuilder: MessagesActionBuilder,
                                                               actionBuilder: DefaultActionBuilder,
                                                               parsers: PlayBodyParsers,
                                                               messagesApi: MessagesApi,
                                                               langs: Langs,
                                                               fileMimeTypes: FileMimeTypes,
                                                               executionContext: ExecutionContext,
                                                               sessionRepository: SessionRepository,
                                                               identify: IdentifierAction,
                                                               getRegistration: GetRegistrationAction,
                                                               getData: DataRetrievalActionProvider,
                                                               requireData: DataRequiredAction,
                                                               checkBouncedEmail: CheckBouncedEmailFilterProvider,
                                                               requirePreviousReturns: CheckSubmittedReturnsFilterProvider,
                                                               withSavedAnswers: SavedAnswersRetrievalActionProvider,
                                                               checkExcludedTrader: CheckExcludedTraderFilter,
                                                               checkExcludedTraderOptional: CheckExcludedTraderOptionalFilter,
                                                               checkCommencementDate: CheckCommencementDateFilter,
                                                               checkCommencementDateOptional: CheckCommencementDateOptionalFilter,
                                                               checkIsCurrentReturnPeriodFilter: CheckIsCurrentReturnPeriodFilter
                                                             ) extends AuthenticatedControllerComponents
