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

package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.{JsError, JsNumber, JsString, Json, JsonValidationError, __}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

import scala.language.postfixOps

class VatOnSalesChoiceSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  val messagesApi: MessagesApi = stubMessagesApi()
  implicit val messages: Messages = messagesApi.preferred(FakeRequest())

  "VatOnSales" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(VatOnSalesChoice.values.toSeq)

      forAll(gen) {
        vatOnSales =>

          JsString(vatOnSales.toString).validate[VatOnSalesChoice].asOpt.value mustEqual vatOnSales
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!VatOnSalesChoice.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[VatOnSalesChoice] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(VatOnSalesChoice.values.toSeq)

      forAll(gen) {
        vatOnSales =>

          Json.toJson(vatOnSales) mustEqual JsString(vatOnSales.toString)
      }
    }

    "generate the correct number of RadioItem options" in {
      val options = VatOnSalesChoice.options

      options.size mustEqual 2

      options.head.value mustBe Some("option1")
      options(1).value mustBe Some("option2")
    }

    "generate correct RadioItem content, value, and id" in {
      val options = VatOnSalesChoice.options

      options.head.content mustBe Text(messages("vatOnSales.option1"))
      options.head.value mustBe Some("option1")
      options.head.id mustBe Some("value_0")

      options(1).content mustBe Text(messages("vatOnSales.option2"))
      options(1).value mustBe Some("option2")
      options(1).id mustBe Some("value_1")
    }

    "must fail to deserialise with missing fields" in {
      val jsonMissingChoice = Json.obj("amount" -> JsNumber(100.50))
      val jsonMissingAmount = Json.obj("choice" -> JsString("option1"))

      jsonMissingChoice.validate[VatOnSales] mustEqual JsError(__ \ "choice" -> JsonValidationError("error.path.missing"))
      jsonMissingAmount.validate[VatOnSales] mustEqual JsError(__ \ "amount" -> JsonValidationError("error.path.missing"))
    }

    "must fail to deserialise with invalid amount" in {
      val jsonInvalidAmount = Json.obj(
        "choice" -> JsString("option1"),
        "amount" -> JsString("not_a_number")
      )

      jsonInvalidAmount.validate[VatOnSales] mustEqual JsError(__ \ "amount" -> JsonValidationError("error.expected.numberformatexception"))
    }
  }
}
