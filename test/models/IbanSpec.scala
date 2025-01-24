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

import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class IbanSpec extends AnyFreeSpec with Matchers with EitherValues {

  ".apply" - {

    "must create an Iban given known valid inputs" in {

      val inputs = Set(
        "GB94BARC10201530093459",
        "GB33BUKB20201555555555",
        "DE29100100100987654321",
        "GB24BKEN10000031510604",
        "GB27BOFI90212729823529",
        "GB17BOFS80055100813796",
        "GB92BARC20005275849855",
        "GB66CITI18500812098709",
        "GB15CLYD82663220400952",
        "GB26MIDL40051512345674",
        "GB76LOYD30949301273801",
        "GB25NWBK60080600724890",
        "GB60NAIA07011610909132",
        "GB29RBOS83040210126939",
        "GB79ABBY09012603367219",
        "GB21SCBL60910417068859",
        "GB42CPBK08005470328725"
      )

      for (input <- inputs) {
        Iban(input).value.toString mustEqual input
      }
    }

    "must not create an Iban from inputs in the wrong format" in {
      val inputs = Set(
        "G94BARC10201530093459",
        "GB33BUKB20153"
      )

      for (input <- inputs) {
        Iban(input) mustEqual Left(IbanError.InvalidFormat)
      }
    }

    "must not create an Iban from inputs with an incorrect checksum" in {
      val inputs = Set(
        "GB01BARC20714583608387",
        "GB00HLFX11016111455365"
      )

      for (input <- inputs) {
        Iban(input) mustEqual Left(IbanError.InvalidChecksum)
      }
    }
  }

  "must serialise and deserialise to / from an Iban" in {

    val iban = Iban("GB94BARC10201530093459").value
    val json = JsString("GB94BARC10201530093459")

    Json.toJson(iban) mustEqual json
    json.validate[Iban] mustEqual JsSuccess(iban)
  }

  "must return JsError when reading invalid IBAN format" in {

    val json = JsString("G294BARC10201530093459")

    json.validate[Iban] mustEqual JsError("IBAN is not in the correct format")
  }

  "must return JsError when reading invalid IBAN checksum" in {

    val json = JsString("GB00BARC10201530093459")

    json.validate[Iban] mustEqual JsError("Invalid checksum")
  }

  "must return JsError when reading invalid IBAN json" in {

    val json = Json.obj("something" -> "GB00BARC10201530093459")

    json.validate[Iban] mustEqual JsError("IBAN is not in the correct format")
  }
}
