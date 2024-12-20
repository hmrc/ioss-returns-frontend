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

package models.core

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json, Reads}
import base.SpecBase

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}
import java.util.UUID


class CoreVatReturnSpec extends SpecBase with Matchers {

  val vatId: CoreEuTraderVatId = CoreEuTraderVatId("VAT123456", "DE")
  val taxId: CoreEuTraderTaxId = CoreEuTraderTaxId("TAX987654", "FR")

  "CoreVatReturn" - {

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(coreVatReturn)
      val deserializedObject = serializedJson.as[CoreVatReturn]

      deserializedObject mustEqual coreVatReturn.copy(
        changeDate = coreVatReturn.changeDate.map(_.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("Z")).toLocalDateTime)
      )
    }

    "ensure all fields are serialized correctly" in {
      val json = Json.toJson(coreVatReturn)

      (json \ "vatReturnReferenceNumber").as[String] mustEqual coreVatReturn.vatReturnReferenceNumber
      (json \ "version").as[Instant] mustEqual coreVatReturn.version
      (json \ "traderId" \ "IOSSNumber").as[String] mustEqual coreVatReturn.traderId.IOSSNumber
      (json \ "period" \ "year").as[Int] mustEqual coreVatReturn.period.year
      (json \ "period" \ "month").as[String] mustEqual coreVatReturn.period.month
      (json \ "startDate").as[LocalDate] mustEqual coreVatReturn.startDate
      (json \ "endDate").as[LocalDate] mustEqual coreVatReturn.endDate
      (json \ "submissionDateTime").as[Instant] mustEqual coreVatReturn.submissionDateTime
      (json \ "totalAmountVatDueGBP").as[BigDecimal] mustEqual coreVatReturn.totalAmountVatDueGBP

      (json \ "msconSupplies").as[List[JsObject]].headOption.map { msconJson =>
        (msconJson \ "msconCountryCode").as[String] mustEqual "DE"
        (msconJson \ "balanceOfVatDueGBP").as[BigDecimal] mustEqual BigDecimal(10)
      }.getOrElse(fail("msconSupplies list is empty"))
    }

    "handle optional changeDate field" - {

      "With changeDate" in {
        val jsonWithChangeDate = Json.toJson(coreVatReturn)
        (jsonWithChangeDate \ "changeDate").asOpt[LocalDateTime] mustBe defined
      }

      "Without changeDate" in {
        val vatReturnWithoutChangeDate = coreVatReturn.copy(changeDate = None)
        val jsonWithoutChangeDate = Json.toJson(vatReturnWithoutChangeDate)
        (jsonWithoutChangeDate \ "changeDate").isDefined mustBe false
      }
    }

    "test invalid JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086",
        "version" -> "Invalid version"
      )

      val result = invalidJson.asOpt[CoreVatReturn]
      result mustBe None
    }

    "serialize and deserialize CoreVatReturn with empty msconSupplies" in {
      val vatReturnWithEmptySupplies = coreVatReturn.copy(msconSupplies = List.empty)
      val serializedJson = Json.toJson(vatReturnWithEmptySupplies)
      val deserializedObject = serializedJson.as[CoreVatReturn]

      deserializedObject.msconSupplies mustBe empty
    }

    "handle boundary values for totalAmountVatDueGBP" in {
      val maxValue = BigDecimal("999999999999.99")
      val vatReturnWithMaxValue = coreVatReturn.copy(totalAmountVatDueGBP = maxValue)

      val serializedJson = Json.toJson(vatReturnWithMaxValue)
      (serializedJson \ "totalAmountVatDueGBP").as[BigDecimal] mustEqual maxValue
    }

    "handle missing fields in JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086"
      )

      val result = invalidJson.validate[CoreVatReturn]
      result.isError mustBe true
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "vatReturnReferenceNumber" -> "XI/XI063407423/M11.2086",
        "version" -> "Invalid Instant"
      )

      val result = invalidJson.validate[CoreVatReturn]
      result.isError mustBe true
    }
  }

  "CorePeriod" - {
    "correctly format the period" in {
      val period = CorePeriod(2021, "03")
      period.toString mustEqual "2021-M03"
    }
  }


  "CoreEuTraderId" - {

    "deserialize a CoreEuTraderId" in {

      val vatJson = Json.obj(
        "vatIdNumber" -> "VAT123456",
        "issuedBy" -> "DE"
      )

      val taxJson = Json.obj(
        "taxRefNumber" -> "TAX987654",
        "issuedBy" -> "FR"
      )

      val vatResult = vatJson.as[CoreEuTraderId]
      vatResult mustBe a[CoreEuTraderVatId]
      vatResult.asInstanceOf[CoreEuTraderVatId].vatIdNumber mustBe "VAT123456"
      vatResult.asInstanceOf[CoreEuTraderVatId].issuedBy mustBe "DE"

      val taxResult = taxJson.as[CoreEuTraderId]
      taxResult mustBe a[CoreEuTraderTaxId]
      taxResult.asInstanceOf[CoreEuTraderTaxId].taxRefNumber mustBe "TAX987654"
      taxResult.asInstanceOf[CoreEuTraderTaxId].issuedBy mustBe "FR"
    }

    "serialize a CoreEuTraderId" in {
      val vatJson = Json.toJson(vatId)
      (vatJson \ "vatIdNumber").as[String] mustBe "VAT123456"
      (vatJson \ "issuedBy").as[String] mustBe "DE"

      val taxJson = Json.toJson(taxId)
      (taxJson \ "taxRefNumber").as[String] mustBe "TAX987654"
      (taxJson \ "issuedBy").as[String] mustBe "FR"
    }

    "handle unknown discriminator in JSON for CoreEuTraderId" in {
      val unknownJson = Json.obj(
        "unknownField" -> "something",
        "issuedBy" -> "DE"
      )

      val result = unknownJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }

    "handle empty JSON for CoreEuTraderId" in {
      val emptyJson = Json.obj()
      val result = emptyJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }
  }

  "CoreEuTraderVatId" - {

    "deserialize a CoreEuTraderVatId correctly" in {
      val json = Json.obj(
        "vatIdNumber" -> "VAT123456",
        "issuedBy" -> "DE"
      )

      val result = json.as[CoreEuTraderId]
      result mustBe a[CoreEuTraderVatId]
      result.asInstanceOf[CoreEuTraderVatId].vatIdNumber mustBe "VAT123456"
      result.asInstanceOf[CoreEuTraderVatId].issuedBy mustBe "DE"
    }

    "serialize a CoreEuTraderVatId correctly" in {
      val json = Json.toJson(vatId)
      (json \ "vatIdNumber").as[String] mustBe "VAT123456"
      (json \ "issuedBy").as[String] mustBe "DE"
    }

    "handle invalid JSON for CoreEuTraderId" in {
      val invalidJson = Json.obj("vatIdNumber" -> "VAT123456")
      val result = invalidJson.validate[CoreEuTraderId]
      result.isError mustBe true
    }
  }

  "CoreEuTraderTaxId" - {

    "deserialize a CoreEuTraderTaxId correctly" in {
      val json = Json.obj(
        "taxRefNumber" -> "TAX987654",
        "issuedBy" -> "FR"
      )

      val result = json.as[CoreEuTraderId]
      result mustBe a[CoreEuTraderTaxId]
      result.asInstanceOf[CoreEuTraderTaxId].taxRefNumber mustBe "TAX987654"
      result.asInstanceOf[CoreEuTraderTaxId].issuedBy mustBe "FR"
    }

    "serialize a CoreEuTraderTaxId correctly" in {
      val json = Json.toJson(taxId)
      (json \ "taxRefNumber").as[String] mustBe "TAX987654"
      (json \ "issuedBy").as[String] mustBe "FR"
    }

    "handle empty JSON for CoreEuTraderId" in {
      val emptyJson = Json.obj()
      val result = emptyJson.validate[CoreEuTraderId]

      result.isError mustBe true
    }
  }

  "CoreMsestSupply" - {

    val supply1 = CoreSupply("Type1", BigDecimal(20.0), "Standard", BigDecimal(100.0), BigDecimal(20.0))
    val supply2 = CoreSupply("Type2", BigDecimal(15.0), "Reduced", BigDecimal(50.0), BigDecimal(7.5))

    val msestSupply = CoreMsestSupply(
      countryCode = Some("DE"),
      euTraderId = Some(vatId),
      supplies = List(supply1, supply2)
    )

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(msestSupply)
      val deserializedObject = serializedJson.as[CoreMsestSupply]

      deserializedObject mustEqual msestSupply
    }

    "ensure all fields are serialized correctly" in {
      val json = Json.toJson(msestSupply)

      (json \ "countryCode").asOpt[String] mustBe Some("DE")
      (json \ "euTraderId").asOpt[JsObject] match {
        case Some(obj) =>
          (obj \ "vatIdNumber").asOpt[String] mustBe Some("VAT123456")
          (obj \ "issuedBy").asOpt[String] mustBe Some("DE")
        case None => fail("euTraderId was not serialized correctly")
      }
      (json \ "supplies").as[List[JsObject]].size mustBe 2
    }

    "handle optional countryCode and euTraderId fields" - {

      "With countryCode and euTraderId" in {
        val jsonWithCountryCodeAndTraderId = Json.toJson(msestSupply)
        (jsonWithCountryCodeAndTraderId \ "countryCode").asOpt[String] mustBe Some("DE")
        (jsonWithCountryCodeAndTraderId \ "euTraderId").asOpt[JsObject] mustBe defined
      }

      "Without countryCode" in {
        val msestSupplyWithoutCountryCode = msestSupply.copy(countryCode = None)
        val jsonWithoutCountryCode = Json.toJson(msestSupplyWithoutCountryCode)
        (jsonWithoutCountryCode \ "countryCode").asOpt[String] mustBe None
      }

      "Without euTraderId" in {
        val msestSupplyWithoutTraderId = msestSupply.copy(euTraderId = None)
        val jsonWithoutTraderId = Json.toJson(msestSupplyWithoutTraderId)
        (jsonWithoutTraderId \ "euTraderId").asOpt[JsObject] mustBe None
      }
    }

    "handle empty supplies list" in {
      val msestSupplyWithEmptySupplies = msestSupply.copy(supplies = List.empty)
      val serializedJson = Json.toJson(msestSupplyWithEmptySupplies)
      val deserializedObject = serializedJson.as[CoreMsestSupply]

      deserializedObject.supplies mustBe empty
    }

    "test invalid JSON" in {
      val invalidJson = Json.obj(
        "countryCode" -> "DE",
        "euTraderId" -> Json.obj("vatIdNumber" -> "VAT123456")
      )

      val result = invalidJson.asOpt[CoreMsestSupply]
      result mustBe None
    }

    "handle missing fields in JSON" in {
      val invalidJson = Json.obj(
        "countryCode" -> "DE"
      )

      val result = invalidJson.validate[CoreMsestSupply]
      result.isError mustBe true
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "countryCode" -> "DE",
        "euTraderId" -> Json.obj("vatIdNumber" -> 123456)
      )

      val result = invalidJson.validate[CoreMsestSupply]
      result.isError mustBe true
    }
  }

  "CoreErrorResponse" - {

    implicit val uuidReads: Reads[UUID] = Reads[UUID](js => js.validate[String].map(UUID.fromString))

    implicit val optionUUIDReads: Reads[Option[UUID]] = Reads.optionWithNull[UUID](uuidReads)

    val errorResponse = CoreErrorResponse(
      timestamp = Instant.now(),
      transactionId = Some(UUID.randomUUID()),
      errorCode = "OSS_009",
      errorMessage = "Registration not found"
    )

    "serialize and deserialize correctly" in {
      val serializedJson = Json.toJson(errorResponse)
      val deserializedObject = serializedJson.as[CoreErrorResponse]

      deserializedObject mustEqual errorResponse
    }

    "ensure all fields are serialized correctly" in {
      val json = Json.toJson(errorResponse)

      (json \ "timestamp").as[Instant] mustEqual errorResponse.timestamp
      (json \ "transactionId").as[Option[UUID]] mustEqual errorResponse.transactionId
      (json \ "errorCode").as[String] mustEqual errorResponse.errorCode
      (json \ "errorMessage").as[String] mustEqual errorResponse.errorMessage
    }

    "handle missing optional transactionId" in {
      val errorResponseWithoutTransactionId = errorResponse.copy(transactionId = None)
      val serializedJson = Json.toJson(errorResponseWithoutTransactionId)
      val deserializedObject = serializedJson.as[CoreErrorResponse]

      deserializedObject.transactionId mustBe None
    }

    "create exception from error response" in {
      val exception = errorResponse.asException
      exception.getMessage must include(errorResponse.errorMessage)
      exception.getMessage must include(errorResponse.errorCode)
    }

    "test invalid JSON" in {
      val invalidJson = Json.obj(
        "timestamp" -> "Invalid timestamp",
        "errorCode" -> "OSS_009"
      )

      val result = invalidJson.asOpt[CoreErrorResponse]
      result mustBe None
    }

    "handle missing fields in JSON" in {
      val invalidJson = Json.obj(
        "timestamp" -> Instant.now().toString,
        "errorCode" -> "OSS_009"
      )

      val result = invalidJson.validate[CoreErrorResponse]
      result.isError mustBe true
    }

    "handle invalid field types in JSON" in {
      val invalidJson = Json.obj(
        "timestamp" -> Instant.now().toString,
        "transactionId" -> "Invalid UUID", // Invalid field type for transactionId
        "errorCode" -> "OSS_009",
        "errorMessage" -> "Some error"
      )

      val result = invalidJson.validate[CoreErrorResponse]
      result.isError mustBe true
    }
  }
}
