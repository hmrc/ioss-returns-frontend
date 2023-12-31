/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.corrections

import forms.mappings.Mappings
import models.Index
import play.api.data.Form

import javax.inject.Inject

class CorrectionReturnPeriodFormProvider @Inject() extends Mappings {

  val validValues: Seq[String] = Seq("JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE","JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER", "DECEMBER")

  def apply(index: Index, existingAnswers: Seq[String]): Form[String] =
    Form(
      "value" -> text("correctionReturnPeriod.error.required")
        .verifying("error.invalid", value => validValues.contains(value.toUpperCase))
    )
}
