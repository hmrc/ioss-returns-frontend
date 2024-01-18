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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class EtmpObligationsSpec extends SpecBase {

  private val referenceNumber: String = arbitraryObligations.arbitrary.sample.value.obligations.head.referenceNumber
  private val referenceType: String = arbitraryObligations.arbitrary.sample.value.obligations.head.referenceType
  private val obligationDetails: Seq[EtmpObligationDetails] = arbitraryObligations.arbitrary.sample.value.obligations.head.obligationDetails


  "EtmpObligations" - {

    "must deserialise/serialise to and from EtmpObligations" in {

      val json = Json.obj(
        "obligations" -> Json.arr(
          Json.obj(
            "referenceNumber" -> referenceNumber,
            "referenceType" -> referenceType,
            "obligationDetails" -> obligationDetails.map { obligationDetail =>
              Json.obj(
                "status" -> obligationDetail.status,
                "periodKey" -> obligationDetail.periodKey
              )
            }
          )
        )
      )

      val expectedResult = EtmpObligations(obligations = Seq(EtmpObligation(
        referenceNumber = referenceNumber,
        referenceType = referenceType,
        obligationDetails = obligationDetails
      )))

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligations] mustBe JsSuccess(expectedResult)
    }
  }
}
