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

package queries

import models.Index
import pages.PageConstants.{corrections, correctionsToCountry}
import play.api.libs.json.{JsObject, JsPath}

case class DeriveNumberOfCorrections(periodIndex: Index) extends Derivable[Seq[JsObject], Int] {

  override val derive: Seq[JsObject] => Int = _.size

  override def path: JsPath = JsPath \ corrections \ periodIndex.position \ correctionsToCountry
}
