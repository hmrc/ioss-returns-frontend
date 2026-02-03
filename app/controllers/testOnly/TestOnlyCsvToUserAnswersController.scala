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

package controllers.testOnly

import controllers.actions.AuthenticatedControllerComponents
import logging.Logging
import pages.Waypoints
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.fileUpload.CsvParserService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestOnlyCsvToUserAnswersController @Inject()(
                                    cc: AuthenticatedControllerComponents,
                                    csvParser: CsvParserService
                                  )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  protected val controllerComponents: MessagesControllerComponents = cc
  
  def populateUserAnswersFromCsv(waypoints: Waypoints): Action[AnyContent] = cc.authAndRequireData().async {
    implicit request =>
      
      Future
        .fromTry(csvParser.populateUserAnswersFromCsv(request.userAnswers))
        .flatMap { updatedAnswers =>
          cc.sessionRepository.set(updatedAnswers).map { _ =>
            Redirect(controllers.routes.CheckYourAnswersController.onPageLoad(waypoints))
          }
        }
        .recover { case _ =>
          logger.error("Failed to populate user answers from CSV")
          InternalServerError
      }
  }
    
}
