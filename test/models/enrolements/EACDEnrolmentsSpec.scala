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

package models.enrolements

import base.SpecBase
import models.enrolments.{EACDEnrolment, EACDEnrolments, EACDIdentifiers}
import play.api.libs.json.{JsError, Json}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EACDEnrolmentsSpec extends SpecBase {

  "EACDEnrolments" - {

    "correctly read a valid JSON object into an EACDEnrolments" in {
      val json = Json.parse("""
        {
          "service": "ServiceName",
          "state": "Active",
          "activationDate": "2023-12-12 15:30:45.123",
          "identifiers": [
            { "key": "id1", "value": "value1" }
          ]
        }
      """.stripMargin)

      val expectedDateTime = LocalDateTime.parse("2023-12-12 15:30:45.123", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
      val expectedEnrolment = EACDEnrolment(
        service = "ServiceName",
        state = "Active",
        activationDate = Some(expectedDateTime),
        identifiers = Seq(EACDIdentifiers("id1", "value1"))
      )

      json.as[EACDEnrolment] mustEqual expectedEnrolment
    }

    "correctly handle missing activationDate as None" in {
      val json = Json.parse("""
        {
          "service": "ServiceName",
          "state": "Inactive",
          "activationDate": null,
          "identifiers": [
            { "key": "id2", "value": "value2" }
          ]
        }
      """.stripMargin)

      val expectedEnrolment = EACDEnrolment(
        service = "ServiceName",
        state = "Inactive",
        activationDate = None,
        identifiers = Seq(EACDIdentifiers("id2", "value2"))
      )

      json.as[EACDEnrolment] mustEqual expectedEnrolment
    }
  }

  "EACDEnrolment" - {

    "correctly write an EACDEnrolment object to JSON" in {
      val enrolment = EACDEnrolment(
        service = "ServiceName",
        state = "Active",
        activationDate = Some(LocalDateTime.parse("2023-12-12 15:30:45.123", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))),
        identifiers = Seq(EACDIdentifiers("id1", "value1"))
      )

      val expectedJson = Json.parse("""
        {
          "service": "ServiceName",
          "state": "Active",
          "activationDate": "2023-12-12 15:30:45.123",
          "identifiers": [
            { "key": "id1", "value": "value1" }
          ]
        }
      """.stripMargin)

      Json.toJson(enrolment) mustEqual expectedJson
    }

    "correctly handle None for activationDate in JSON output" in {
      val enrolment = EACDEnrolment(
        service = "ServiceName",
        state = "Inactive",
        activationDate = None,
        identifiers = Seq(EACDIdentifiers("id2", "value2"))
      )

      val expectedJson = Json.parse("""
        {
          "service": "ServiceName",
          "state": "Inactive",
          "identifiers": [
            { "key": "id2", "value": "value2" }
          ]
        }
      """.stripMargin)

      Json.toJson(enrolment) mustEqual expectedJson
    }
  }

  "EACDEnrolments" - {

    "correctly read a valid JSON object into EACDEnrolments" in {
      val json = Json.parse("""
        {
          "enrolments": [
            {
              "service": "ServiceName",
              "state": "Active",
              "activationDate": "2023-12-12 15:30:45.123",
              "identifiers": [
                { "key": "id1", "value": "value1" }
              ]
            },
            {
              "service": "AnotherService",
              "state": "Inactive",
              "activationDate": null,
              "identifiers": [
                { "key": "id2", "value": "value2" }
              ]
            }
          ]
        }
      """.stripMargin)

      val expectedEnrolments = EACDEnrolments(
        enrolments = Seq(
          EACDEnrolment(
            service = "ServiceName",
            state = "Active",
            activationDate = Some(LocalDateTime.parse("2023-12-12 15:30:45.123", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))),
            identifiers = Seq(EACDIdentifiers("id1", "value1"))
          ),
          EACDEnrolment(
            service = "AnotherService",
            state = "Inactive",
            activationDate = None,
            identifiers = Seq(EACDIdentifiers("id2", "value2"))
          )
        )
      )

      json.as[EACDEnrolments] mustEqual expectedEnrolments
    }

    "correctly write EACDEnrolments to JSON" in {
      val enrolments = EACDEnrolments(
        enrolments = Seq(
          EACDEnrolment(
            service = "ServiceName",
            state = "Active",
            activationDate = Some(LocalDateTime.parse("2023-12-12 15:30:45.123", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))),
            identifiers = Seq(EACDIdentifiers("id1", "value1"))
          ),
          EACDEnrolment(
            service = "AnotherService",
            state = "Inactive",
            activationDate = None,
            identifiers = Seq(EACDIdentifiers("id2", "value2"))
          )
        )
      )

      val expectedJson = Json.parse("""
        {
          "enrolments": [
            {
              "service": "ServiceName",
              "state": "Active",
              "activationDate": "2023-12-12 15:30:45.123",
              "identifiers": [
                { "key": "id1", "value": "value1" }
              ]
            },
            {
              "service": "AnotherService",
              "state": "Inactive",
              "identifiers": [
                { "key": "id2", "value": "value2" }
              ]
            }
          ]
        }
      """.stripMargin)

      Json.toJson(enrolments) mustEqual expectedJson
    }

    "correctly handle invalid Json" in {
      val expectedJson = Json.obj()

      expectedJson.validate[EACDEnrolments] mustBe a[JsError]
    }

    "must handle invalid data during deserialization" in {

      val expectedJson = Json.obj(
        "enrolments" -> Json.obj(
          "service" -> 123456789,
          "state" -> "Active",
          "activationDate" -> "2023-12-12 15:30:45.123",
          "identifiers" -> Json.obj(
            "key" -> "id1",
            "value" -> "value1"
          )
        )
      )

      expectedJson.validate[EACDEnrolments] mustBe a[JsError]
    }
  }
}
