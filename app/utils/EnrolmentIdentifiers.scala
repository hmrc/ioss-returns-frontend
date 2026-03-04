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

import uk.gov.hmrc.auth.core.Enrolments

object EnrolmentIdentifiers {
  
  def findIossFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-ORG")
      .flatMap(_.identifiers.find(id => id.key == "IOSSNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }

  def findIntermediaryFromEnrolments(enrolments: Enrolments): Seq[String] = {
    enrolments.enrolments
      .filter(_.key == "HMRC-IOSS-INT")
      .flatMap(_.identifiers.find(id => id.key == "IntNumber" && id.value.nonEmpty).map(_.value)).toSeq
  }
}
