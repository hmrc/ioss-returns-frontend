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

package viewmodels.payments

import models.payments.PrepareData
import play.api.libs.json.{OFormat, OWrites, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SelectedPrepareData(userId: String, prepareData: PrepareData, lastUpdated: Instant = Instant.now)

object SelectedPrepareData {

  val reads: Reads[SelectedPrepareData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").read[String] and
        (__ \ "prepareData").read[PrepareData] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      )(SelectedPrepareData.apply _)
  }

  val writes: OWrites[SelectedPrepareData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").write[String] and
        (__ \ "prepareData").write[PrepareData] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      )(unlift(SelectedPrepareData.unapply))
  }

  implicit val format: OFormat[SelectedPrepareData] = OFormat(reads, writes)
}

