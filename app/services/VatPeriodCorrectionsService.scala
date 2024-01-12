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

package services

import connectors.VatReturnConnector
import models.Period
import models.corrections.PeriodWithCorrections
import models.etmp.EtmpObligationDetails
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VatPeriodCorrectionsService @Inject()(vatReturnConnector: VatReturnConnector)(implicit ec: ExecutionContext) {

  def getCorrectionPeriodsWhereCountryVatCorrectionMissesAtLeastOnce(periodWithCorrections: Option[List[PeriodWithCorrections]]): List[Period] = {
    periodWithCorrections
      .map(_.filter(_.correctionsToCountry.getOrElse(List.empty).exists(_.countryVatCorrection.isEmpty)))//Corrections where vatcorrections has not been entered
      .map(_.map(_.correctionReturnPeriod))
      .getOrElse(List())
  }

  def listStatuses(iossNumber: String)(implicit hc: HeaderCarrier): Future[Seq[EtmpObligationDetails]] =
    vatReturnConnector.getObligations(iossNumber).map(_.obligationDetails)

}
