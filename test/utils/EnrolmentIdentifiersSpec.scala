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

package utils

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

class EnrolmentIdentifiersSpec extends SpecBase with Matchers {

  ".findIossFromEnrolments" - {

    "return IOSS number when HMRC-IOSS-ORG enrolment exists with valid identifier" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "HMRC-IOSS-ORG",
          identifiers = Seq(
            EnrolmentIdentifier("IOSSNumber", "IM9001234567")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIossFromEnrolments(enrolments)

      result mustBe Seq("IM9001234567")
    }

    "return empty sequence when enrolment exists but identifier value is empty" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "HMRC-IOSS-ORG",
          identifiers = Seq(
            EnrolmentIdentifier("IOSSNumber", "")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIossFromEnrolments(enrolments)

      result mustBe Seq.empty
    }

    "return empty sequence when HMRC-IOSS-ORG enrolment does not exist" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "SOME-OTHER-KEY",
          identifiers = Seq(
            EnrolmentIdentifier("IOSSNumber", "IM9001234567")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIossFromEnrolments(enrolments)

      result mustBe Seq.empty
    }
  }

  ".findIntermediaryFromEnrolments" - {

    "return Intermediary number when HMRC-IOSS-INT enrolment exists with valid identifier" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "HMRC-IOSS-INT",
          identifiers = Seq(
            EnrolmentIdentifier("IntNumber", "IN9001234567")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIntermediaryFromEnrolments(enrolments)

      result mustBe Seq("IN9001234567")
    }

    "return empty sequence when enrolment exists but identifier value is empty" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "HMRC-IOSS-INT",
          identifiers = Seq(
            EnrolmentIdentifier("IntNumber", "")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIntermediaryFromEnrolments(enrolments)

      result mustBe Seq.empty
    }

    "return empty sequence when HMRC-IOSS-INT enrolment does not exist" in {

      val enrolments = Enrolments(Set(
        Enrolment(
          key = "SOME-OTHER-KEY",
          identifiers = Seq(
            EnrolmentIdentifier("IntNumber", "IN9001234567")
          ),
          state = "Activated"
        )
      ))

      val result = EnrolmentIdentifiers.findIntermediaryFromEnrolments(enrolments)

      result mustBe Seq.empty
    }
  }
}
