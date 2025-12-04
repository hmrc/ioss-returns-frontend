/*
 * Copyright 2025 HM Revenue & Customs
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

package models.etmp.intermediary

import models.{Enumerable, WithName}

sealed trait EtmpIdType

object EtmpIdType extends Enumerable.Implicits {

  case object VRN extends WithName("VRN") with EtmpIdType
  case object NINO extends WithName("NINO") with EtmpIdType
  case object UTR extends WithName("UTR") with EtmpIdType
  case object FTR extends WithName("FTR") with EtmpIdType

  val values: Seq[EtmpIdType] = Seq(
    VRN,
    NINO,
    UTR,
    FTR
  )

  implicit val enumerable: Enumerable[EtmpIdType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}