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

package controllers.corrections

import controllers.JourneyRecoverySyntax._
import models.Index
import models.requests.DataRequest
import pages.corrections.{CorrectionReturnPeriodPage, CorrectionReturnYearPage}
import play.api.mvc.{AnyContent, Result}
import queries.DeriveNumberOfCorrections

import scala.concurrent.Future

trait VatCorrectionBaseController {


  protected def getNumberOfCorrections(periodIndex: Index)
                                      (block: (Int, String) => Result)
                                      (implicit request: DataRequest[AnyContent]): Result =

    (for {
      numberOfCorrections <- request.userAnswers.get(DeriveNumberOfCorrections(periodIndex))
      correctionPeriod    <- request.userAnswers.get(CorrectionReturnPeriodPage[String](periodIndex))
      correctionYear      <- request.userAnswers.get(CorrectionReturnYearPage(periodIndex))
    } yield block(numberOfCorrections, s"$correctionPeriod $correctionYear"))
      .orRecoverJourney

  protected def getNumberOfCorrectionsAsync(periodIndex: Index)
                                      (block: (Int, String) => Future[Result])
                                      (implicit request: DataRequest[AnyContent]): Future[Result] =

    (for {
      numberOfCorrections <- request.userAnswers.get(DeriveNumberOfCorrections(periodIndex))
      correctionPeriod    <- request.userAnswers.get(CorrectionReturnPeriodPage[String](periodIndex))
      correctionYear      <- request.userAnswers.get(CorrectionReturnYearPage(periodIndex))
    } yield block(numberOfCorrections, s"$correctionPeriod $correctionYear"))
      .orRecoverJourney
}
