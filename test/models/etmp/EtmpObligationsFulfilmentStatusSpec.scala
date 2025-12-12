/*
 * Copyright 2025 HM Revenue & Customs
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

package models.etmp

import base.SpecBase
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class EtmpObligationsFulfilmentStatusSpec extends SpecBase with ScalaCheckPropertyChecks {

  "EtmpObligationsFulfilmentStatus" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(EtmpObligationsFulfilmentStatus.values)

      forAll(gen) {
        obligationFulfilmentStatus =>

          JsString(obligationFulfilmentStatus.toString)
            .validate[EtmpObligationsFulfilmentStatus].asOpt.value mustBe obligationFulfilmentStatus
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String].suchThat(!EtmpObligationsFulfilmentStatus.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValues =>

          JsString(invalidValues).validate[EtmpObligationsFulfilmentStatus] mustBe JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(EtmpObligationsFulfilmentStatus.values)

      forAll(gen) {
        obligationFulfilmentStatus =>

          Json.toJson(obligationFulfilmentStatus) mustBe JsString(obligationFulfilmentStatus.toString)
      }
    }
  }
}
