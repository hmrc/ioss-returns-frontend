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

package pages

import connectors.VatReturnConnector
import controllers.actions.AuthenticatedControllerComponents
import models.{Index, StandardPeriod}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.behaviours.PageBehaviours
import pages.corrections.{CorrectionReturnPeriodPage, VatPeriodCorrectionsListPage}
import play.api.inject.bind

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success


class VatPeriodCorrectionsListPageSpec extends PageBehaviours {

  "VatPeriodCorrectionsListPage" - {
    "must navigate in Normal mode" - {

      "when the answer is yes" - {

        "to CorrectionReturnPeriod when there are already some corrections" in {

          val answers =
            emptyUserAnswers
              .set(CorrectionReturnPeriodPage(index), period).success.value

          VatPeriodCorrectionsListPage(period, addAnother = true).navigate(waypoints, answers, answers).route
            .mustEqual(controllers.corrections.routes.CorrectionReturnYearController.onPageLoad(waypoints, Index(1)))
        }

        "to CorrectionReturnPeriod when there are no corrections" in {

          val answers =
            emptyUserAnswers

          VatPeriodCorrectionsListPage(period, addAnother = true).navigate(waypoints, answers, answers).route
            .mustEqual(controllers.corrections.routes.CorrectionReturnYearController.onPageLoad(waypoints, Index(0)))
        }
      }

      "when the answer is no" - {

        "to CheckYourAnswers" in {
          val answers =
            emptyUserAnswers
              .set(CorrectionReturnPeriodPage(index), period).success.value
          VatPeriodCorrectionsListPage(period, addAnother = false).navigate(waypoints, answers, answers).route
            .mustEqual(controllers.routes.CheckYourAnswersController.onPageLoad())
        }
      }
    }

    "cleanup" -{
      "must delete empty periods" in {
        val mockReturnStatusConnector = mock[VatReturnConnector]

        val answers = emptyUserAnswers
          .set(CorrectionReturnPeriodPage(index), StandardPeriod("2021", "7").get).success.value
          .set(CorrectionReturnPeriodPage(Index(1)), StandardPeriod("2022", "1").get).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockReturnStatusConnector))
          .build()
        val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

        val result = VatPeriodCorrectionsListPage(period, addAnother = false).cleanup(answers, cc)
        result.futureValue mustEqual Success(emptyUserAnswers)
      }
    }
  }
}
