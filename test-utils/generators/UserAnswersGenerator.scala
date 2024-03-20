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

package generators

import models.UserAnswers
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.TryValues
import pages.QuestionPage
import pages.corrections._
import play.api.libs.json.{JsValue, Json}

import java.time.Month

trait UserAnswersGenerator extends TryValues {
  self: Generators =>
  implicit lazy val arbitraryUndeclaredCountryCorrectionUserAnswersEntry: Arbitrary[(UndeclaredCountryCorrectionPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[UndeclaredCountryCorrectionPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCountryVatCorrectionUserAnswersEntry: Arbitrary[(VatAmountCorrectionCountryPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[VatAmountCorrectionCountryPage]
        value <- arbitrary[Int].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCorrectionCountryUserAnswersEntry: Arbitrary[(CorrectionCountryPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[CorrectionCountryPage]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCorrectPreviousReturnUserAnswersEntry: Arbitrary[(CorrectPreviousReturnPage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[CorrectPreviousReturnPage.type]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }


  val generators: Seq[Gen[(QuestionPage[_], JsValue)]] =
    arbitrary[(UndeclaredCountryCorrectionPage, JsValue)] ::
    arbitrary[(VatAmountCorrectionCountryPage, JsValue)] ::
    Nil

  implicit lazy val arbitraryUserData: Arbitrary[UserAnswers] = {

    import models._

    Arbitrary {
      for {
        id      <- nonEmptyString
        data    <- generators match {
          case Nil => Gen.const(Map[QuestionPage[_], JsValue]())
          case _   => Gen.mapOf(oneOf(generators))
        }
      } yield UserAnswers (
        id           = id,
        period       = StandardPeriod(2021, Month.JULY),
        data         = data.foldLeft(Json.obj()) {
          case (obj, (path, value)) =>
            obj.setObject(path.path, value).get
        }
      )
    }
  }

}
