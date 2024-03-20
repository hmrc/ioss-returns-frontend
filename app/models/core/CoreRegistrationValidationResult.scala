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

import play.api.libs.json.{Json, OFormat}

import java.time.format.DateTimeFormatter

case class CoreRegistrationValidationResult(
                                             searchId: String,
                                             searchIdIntermediary: Option[String],
                                             searchIdIssuedBy: String,
                                             traderFound: Boolean,
                                             matches: Seq[Match]
                                           )

object CoreRegistrationValidationResult {

  implicit val format: OFormat[CoreRegistrationValidationResult] = Json.format[CoreRegistrationValidationResult]

}

case class Match(
                  matchType: MatchType,
                  traderId: String,
                  intermediary: Option[String],
                  memberState: String,
                  exclusionStatusCode: Option[Int],
                  exclusionDecisionDate: Option[String],
                  exclusionEffectiveDate: Option[String],
                  nonCompliantReturns: Option[Int],
                  nonCompliantPayments: Option[Int]
                )

object Match {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd")

  implicit val format: OFormat[Match] = Json.format[Match]

}