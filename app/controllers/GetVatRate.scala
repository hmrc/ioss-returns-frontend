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

package controllers

import logging.Logging
import models.Index
import models.requests.DataRequest
import play.api.mvc.AnyContent
import queries.VatRateFromCountryQuery

trait GetVatRate extends Logging {

  protected def getVatRate(countryIndex: Index, vatRateIndex: Index, request: DataRequest[AnyContent]): String = {
    val vatRate = request.userAnswers.get(VatRateFromCountryQuery(countryIndex, vatRateIndex)) match {
      case Some(vatRate) => vatRate.rateForDisplay
      case _ =>
        val exception = new IllegalStateException("VAT rate missing")
        logger.error(exception.getMessage, exception)
        throw exception
    }
    vatRate
  }
}
