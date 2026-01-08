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

package models.etmp

import base.SpecBase
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}

class EtmpObligationsSpec extends SpecBase {

  private val obligationDetails: Seq[EtmpObligationDetails] = arbitraryObligations.arbitrary.sample.value.obligations.head.obligationDetails


  "EtmpObligations" - {

    "must deserialise/serialise to and from EtmpObligations" in {

      val json = Json.obj(
        "obligations" -> Json.arr(
          Json.obj(
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
        obligationDetails = obligationDetails
      )))

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligations] mustBe JsSuccess(expectedResult)
    }


    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "obligations" -> Json.arr(
          Json.obj(
            "obligationDetails" -> obligationDetails.map { obligationDetail =>
              Json.obj(
                "status" -> obligationDetail.status,
                "periodKey" -> 12345
              )
            }
          )
        )
      )

      json.validate[EtmpObligations] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpObligations] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "obligations" -> Json.arr(
          Json.obj(
            "obligationDetails" -> obligationDetails.map { obligationDetail =>
              Json.obj(
                "status" -> JsNull,
                "periodKey" -> obligationDetail.periodKey
              )
            }
          )
        )
      )

      json.validate[EtmpObligations] mustBe a[JsError]
    }
  }

  "EtmpObligation" - {

    "must deserialise/serialise to and from EtmpObligation" in {

      val json = Json.obj(
        "obligationDetails" -> Json.arr(
          Json.obj(
            "status" -> "F",
            "periodKey" -> "29AH"
          )
        )
      )

      val expectedResult = EtmpObligation(
        obligationDetails = Seq(
          EtmpObligationDetails(
            status = EtmpObligationsFulfilmentStatus.Fulfilled,
            periodKey = "29AH"
          )
        )
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[EtmpObligation] mustBe JsSuccess(expectedResult)
    }


    "must handle invalid data during deserialization" in {

      val json = Json.obj(
        "obligationDetails" -> Json.arr(
          Json.obj(
            "status" -> 1,
            "periodKey" -> "29AH"
          )
        )
      )

      json.validate[EtmpObligation] mustBe a[JsError]
    }

    "must handle missing fields during deserialization" in {

      val json = Json.obj()

      json.validate[EtmpObligation] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val json = Json.obj(
        "obligationDetails" -> Json.arr(
          Json.obj(
            "status" -> JsNull,
            "periodKey" -> "29AH"
          )
        )
      )

      json.validate[EtmpObligation] mustBe a[JsError]
    }
  }
}
