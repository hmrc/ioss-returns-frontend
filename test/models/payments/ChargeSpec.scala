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

package models.payments

import base.SpecBase
import models.Period
import models.payments.Charge._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import java.time.Month

class ChargeSpec extends SpecBase with ScalaCheckPropertyChecks {

  "Charge" - {
    "must" - {
      val chargeOutStandingAmountEqualToOriginalAmount = Charge(Period(2020, Month.MAY), BigDecimal(10), BigDecimal(10), BigDecimal(10))

      val chargeOutStandingAmountNotEqualToOriginalAmount = Charge(Period(2020, Month.MAY), BigDecimal(10), BigDecimal(7), BigDecimal(3))

      val scenarios = Table[Option[Charge], PaymentStatus](
        ("Charge", "Payment Status"),
        (
          None,
          PaymentStatus.Unknown
        ),
        (
          Some(chargeOutStandingAmountEqualToOriginalAmount),
          PaymentStatus.Unpaid
        ),
        (
          Some(chargeOutStandingAmountNotEqualToOriginalAmount),
          PaymentStatus.Partial
        )
      )

      forAll(scenarios) { (c, ps) =>
        s"correctly detect Payment Status $ps" in {
          c.getPaymentStatus() mustBe ps
        }
      }
    }
  }
}
