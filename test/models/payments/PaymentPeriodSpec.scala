package models.payments

import base.SpecBase
import play.api.libs.json.{Json, JsSuccess}

import java.time.Month

class PaymentPeriodSpec extends SpecBase {

//  private val paymentPeriod: PaymentPeriod = arbitraryPaymentPeriod.sample.value

  "PaymentPeriod" - {

    "must deserialise/serialise to and from PaymentPeriod" in {

      val json = Json.obj(
        "year" -> 2023,
        "month" -> 11
      )

      val expectedResult = PaymentPeriod(
        year = 2023,
        month = Month.NOVEMBER
      )

      json mustBe Json.toJson(expectedResult)
      json.validate[PaymentPeriod] mustBe JsSuccess(expectedResult)
    }
  }

}
