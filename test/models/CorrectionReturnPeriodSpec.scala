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

package models

import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class CorrectionReturnPeriodSpec extends AnyFreeSpec with Matchers with MockitoSugar {

  "CorrectionReturnPeriod should" - {

    "have the correct values" in {
      CorrectionReturnPeriod.values mustEqual Seq(CorrectionReturnPeriod.Period1, CorrectionReturnPeriod.Period2, CorrectionReturnPeriod.Period3)
    }

    "generate correct radio options" in {
      val mockMessages = mock[Messages]

      when(mockMessages("correctionReturnPeriod.period1")).thenReturn("Period 1")
      when(mockMessages("correctionReturnPeriod.period2")).thenReturn("Period 2")
      when(mockMessages("correctionReturnPeriod.period3")).thenReturn("Period 3")

      val options = CorrectionReturnPeriod.options(mockMessages)

      options must have size 3

      options.head.content mustEqual Text("Period 1")
      options(1).content mustEqual Text("Period 2")
      options(2).content mustEqual Text("Period 3")

      options.head.id mustBe Some("value_0")
      options(1).id mustBe Some("value_1")
      options(2).id mustBe Some("value_2")

      options.head.value mustBe Some("period1")
      options(1).value mustBe Some("period2")
      options(2).value mustBe Some("period3")
    }

    "correctly map strings to case objects" in {
      val enumerable = CorrectionReturnPeriod.enumerable

      enumerable.withName("period1") mustBe Some(CorrectionReturnPeriod.Period1)
      enumerable.withName("period2") mustBe Some(CorrectionReturnPeriod.Period2)
      enumerable.withName("period3") mustBe Some(CorrectionReturnPeriod.Period3)

      enumerable.withName("invalid") mustBe None
    }
  }
}
