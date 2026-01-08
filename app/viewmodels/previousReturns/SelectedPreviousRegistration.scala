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

package viewmodels.previousReturns

import play.api.libs.json.{__, OFormat, OWrites, Reads}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SelectedPreviousRegistration(userId: String, previousRegistration: PreviousRegistration, lastUpdated: Instant = Instant.now)

object SelectedPreviousRegistration {

  val reads: Reads[SelectedPreviousRegistration] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").read[String] and
        (__ \ "previousRegistration").read[PreviousRegistration] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      ) (SelectedPreviousRegistration.apply _)
  }

  val writes: OWrites[SelectedPreviousRegistration] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "userId").write[String] and
        (__ \ "previousRegistration").write[PreviousRegistration] and
        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
      ) (selectedPreviousRegistration => Tuple.fromProductTyped(selectedPreviousRegistration))
  }

  implicit val format: OFormat[SelectedPreviousRegistration] = OFormat(reads, writes)
}

