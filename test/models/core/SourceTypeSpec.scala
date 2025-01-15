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

package models.core

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsString, JsSuccess, Json, Reads, Writes}

object SourceTypeJson {
  
  implicit val sourceTypeReads: Reads[SourceType] = {
    case JsString("VATNumber") => JsSuccess(SourceType.VATNumber)
    case JsString("EUTraderId") => JsSuccess(SourceType.EUTraderId)
    case JsString("TraderId") => JsSuccess(SourceType.TraderId)
    case _ => JsError("error.invalid")
  }
  
  implicit val sourceTypeWrites: Writes[SourceType] = Writes {
    case SourceType.VATNumber => JsString("VATNumber")
    case SourceType.EUTraderId => JsString("EUTraderId")
    case SourceType.TraderId => JsString("TraderId")
  }
}

class SourceTypeSpec extends AnyFreeSpec with Matchers with SpecBase {

  "SourceType" - {

    "deserialize valid values" in {
      JsString("VATNumber").validate[SourceType] mustBe JsSuccess(SourceType.VATNumber)
      JsString("EUTraderId").validate[SourceType] mustBe JsSuccess(SourceType.EUTraderId)
      JsString("TraderId").validate[SourceType] mustBe JsSuccess(SourceType.TraderId)
    }

    "fail to deserialize invalid values" in {
      JsString("InvalidSourceType").validate[SourceType] mustBe JsError("error.invalid")
    }

    "serialize valid SourceType values" in {
      Json.toJson(SourceType.VATNumber)(SourceTypeJson.sourceTypeWrites) mustEqual JsString("VATNumber")
      Json.toJson(SourceType.EUTraderId)(SourceTypeJson.sourceTypeWrites) mustEqual JsString("EUTraderId")
      Json.toJson(SourceType.TraderId)(SourceTypeJson.sourceTypeWrites) mustEqual JsString("TraderId")
    }

    "have the correct values" in {
      SourceType.values mustBe Seq(SourceType.VATNumber, SourceType.EUTraderId, SourceType.TraderId)
    }

    "match the correct SourceType for each string" in {
      SourceType.enumerable.withName("VATNumber") mustBe Some(SourceType.VATNumber)
      SourceType.enumerable.withName("EUTraderId") mustBe Some(SourceType.EUTraderId)
      SourceType.enumerable.withName("TraderId") mustBe Some(SourceType.TraderId)
    }

  }

}
