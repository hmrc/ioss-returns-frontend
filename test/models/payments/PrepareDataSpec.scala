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

package models.payments

import base.SpecBase
import org.scalacheck.Gen
import play.api.i18n.Messages
import play.api.libs.json.{JsError, JsNull, JsSuccess, Json}
import play.api.test.Helpers.running
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import viewmodels.govuk.all.currencyFormat

class PrepareDataSpec extends SpecBase {

  private val prepareDataList = Gen.listOfN(3, arbitraryPrepareData.arbitrary).sample.value

  "PrepareData" - {

    "must serialise/deserialise to and from PrepareData" in {

      val prepareData: PrepareData = prepareDataList.head

      val expectedJson = Json.obj(
        "duePayments" -> prepareData.duePayments,
        "overduePayments" -> prepareData.overduePayments,
        "excludedPayments" -> prepareData.excludedPayments,
        "totalAmountOwed" -> prepareData.totalAmountOwed,
        "totalAmountOverdue" -> prepareData.totalAmountOverdue,
        "iossNumber" -> prepareData.iossNumber
      )

      Json.toJson(prepareData) mustBe expectedJson
      expectedJson.validate[PrepareData] mustBe JsSuccess(prepareData)
    }

    "must populate Radio Items correctly" in {

      val application = applicationBuilder().build()

      running(application) {

        implicit val msgs: Messages = messages(application)

        val expectedResult: List[RadioItem] = List(
          RadioItem(
            content = HtmlContent(msgs("whichPreviousRegistrationToPay.selection",
              currencyFormat(prepareDataList.head.totalAmountOverdue), prepareDataList.head.iossNumber)
            ),
            id = Some("value_0"),
            value = Some(prepareDataList.head.iossNumber)
          ),
          RadioItem(
            content = HtmlContent(msgs("whichPreviousRegistrationToPay.selection",
              currencyFormat(prepareDataList.tail.head.totalAmountOverdue), prepareDataList.tail.head.iossNumber)
            ),
            id = Some("value_1"),
            value = Some(prepareDataList.tail.head.iossNumber)
          ),
          RadioItem(
            content = HtmlContent(msgs("whichPreviousRegistrationToPay.selection",
              currencyFormat(prepareDataList.tail.tail.head.totalAmountOverdue), prepareDataList.tail.tail.head.iossNumber)
            ),
            id = Some("value_2"),
            value = Some(prepareDataList.tail.tail.head.iossNumber)
          )
        )

        val result = PrepareData.options(prepareDataList)

        result mustBe expectedResult
      }
    }

    "must handle missing fields during deserialization" in {

      val expectedJson = Json.obj()

      expectedJson.validate[PrepareData] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val prepareData: PrepareData = prepareDataList.head

      val expectedJson = Json.obj(
        "duePayments" -> 12345,
        "overduePayments" -> prepareData.overduePayments,
        "excludedPayments" -> prepareData.excludedPayments,
        "totalAmountOwed" -> prepareData.totalAmountOwed,
        "totalAmountOverdue" -> prepareData.totalAmountOverdue,
        "iossNumber" -> prepareData.iossNumber
      )

      expectedJson.validate[PrepareData] mustBe a[JsError]
    }

    "must handle null data during deserialization" in {

      val expectedJson = Json.obj(
        "duePayments" -> JsNull,
        "overduePayments" -> JsNull,
        "excludedPayments" -> JsNull,
        "totalAmountOwed" -> JsNull,
        "totalAmountOverdue" -> JsNull,
        "iossNumber" -> JsNull
      )

      expectedJson.validate[PrepareData] mustBe a[JsError]
    }
  }
}