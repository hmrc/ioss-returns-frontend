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

package controllers

import java.time.{Clock, LocalDate}

object ExclusionConstraints {

  def hasActiveReturnWindowExpired(dueDate: LocalDate, clock: Clock): Boolean = {
    val today = LocalDate.now(clock)
    today.isAfter(dueDate.plusYears(3))
  }

  def hasActivePaymentWindowExpired(paymentDeadline: LocalDate, clock: Clock): Boolean = {
    val today = LocalDate.now(clock)
    today.isAfter(paymentDeadline.plusYears(3))
  }
}
