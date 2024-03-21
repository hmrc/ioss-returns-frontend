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

import models.{Enumerable, WithName}

sealed trait MatchType

object MatchType extends Enumerable.Implicits {

  case object TraderIdActiveNETP extends WithName("001") with MatchType

  case object TraderIdQuarantinedNETP extends WithName("002") with MatchType

  case object OtherMSNETPActiveNETP extends WithName("003") with MatchType

  case object OtherMSNETPQuarantinedNETP extends WithName("004") with MatchType

  case object FixedEstablishmentActiveNETP extends WithName("005") with MatchType

  case object FixedEstablishmentQuarantinedNETP extends WithName("006") with MatchType

  case object TransferringMSID extends WithName("007") with MatchType

  case object PreviousRegistrationFound extends WithName("008") with MatchType

  val values: Seq[MatchType] = Seq(
    TraderIdActiveNETP,
    TraderIdQuarantinedNETP,
    OtherMSNETPActiveNETP,
    OtherMSNETPQuarantinedNETP,
    FixedEstablishmentActiveNETP,
    FixedEstablishmentQuarantinedNETP,
    TransferringMSID,
    PreviousRegistrationFound
  )

  implicit val enumerable: Enumerable[MatchType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
