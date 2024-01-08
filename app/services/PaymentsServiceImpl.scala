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

package services

import models.Period
import models.payments.{Payment, PaymentStatus}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}

class PaymentsServiceImpl extends PaymentsService { //Todo: To be replaced by the PaymentService of VEIOSS-435
  override def getUnpaidPayments(iossNumber: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Payment]] =
    Future.successful(
      List[Payment](
        Payment(Period.apply(2022, Month.MAY), 234, LocalDate.now().minusYears(1), PaymentStatus.Partial),
        Payment(Period.apply(2023, Month.DECEMBER), 234, LocalDate.now().plusMonths(1), PaymentStatus.Partial)
      )
    )
}

trait PaymentsService {
  def getUnpaidPayments(iossNumber: String)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[List[Payment]]
}
