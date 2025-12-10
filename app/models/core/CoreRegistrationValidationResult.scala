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

import models.exclusions.ExclusionReason
import play.api.libs.json.*

import java.time.{Clock, LocalDate}
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
                  traderId: TraderId,
                  intermediary: Option[String],
                  memberState: String,
                  exclusionStatusCode: Option[Int],
                  exclusionDecisionDate: Option[String],
                  exclusionEffectiveDate: Option[String],
                  nonCompliantReturns: Option[Int],
                  nonCompliantPayments: Option[Int]
                ) {
  def isActiveTrader: Boolean = {
    traderId.isAnIOSSNetp &&
      exclusionStatusCode.isEmpty || exclusionStatusCode.contains(-1)
  }

  def isQuarantinedTrader(clock: Clock): Boolean = {
    (traderId.isAnIOSSNetp || traderId.isAnOSSTrader) &&
      exclusionStatusCode.contains(ExclusionReason.FailsToComply.numberValue) &&
      isEffectiveDateLessThan2YearsAgo(clock)
  }

  private def isEffectiveDateLessThan2YearsAgo(clock: Clock): Boolean = {
    exclusionEffectiveDate.map(LocalDate.parse) match {
      case Some(effectiveDate) =>
        val twoYearsAfterEffective = effectiveDate.plusYears(2)
        val today = LocalDate.now(clock)
        twoYearsAfterEffective.isAfter(today)
      case _ => true
    }
  }
}

object Match {

  val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MM dd")

  implicit val format: OFormat[Match] = Json.format[Match]

}

case class TraderId(traderId: String) {
  private val traderIdScheme = TraderIdScheme(this)

  def isAnIOSSNetp: Boolean = traderIdScheme == TraderIdScheme.ImportOneStopShopNetp

  def isAnOSSTrader: Boolean = traderIdScheme == TraderIdScheme.OneStopShop
}

object TraderId {
  implicit val traderIdReads: Reads[TraderId] = Reads {
    case JsString(value) => JsSuccess(TraderId(value))
    case _ => JsError("Expected string for TraderId")
  }

  implicit val traderIdWrites: Writes[TraderId] = Writes { traderId =>
    JsString(traderId.traderId)
  }

  implicit val traderIdFormat: Format[TraderId] = Format(traderIdReads, traderIdWrites)
}

sealed trait TraderIdScheme

object TraderIdScheme {
  case object OneStopShop extends TraderIdScheme

  case object ImportOneStopShopNetp extends TraderIdScheme

  case object ImportOneStopShopIntermediary extends TraderIdScheme

  def apply(traderId: TraderId): TraderIdScheme = {
    traderId.traderId.toUpperCase match {
      case id if id.startsWith("IN") => ImportOneStopShopIntermediary
      case id if id.startsWith("IM") => ImportOneStopShopNetp
      case _ => OneStopShop
    }
  }
}