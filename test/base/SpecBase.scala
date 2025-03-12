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

package base

import controllers.actions.*
import generators.{Generators, UserAnswersGenerator}
import models.*
import models.core._
import models.etmp.{DesAddress, VatCustomerInfo}
import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.corrections.{CorrectPreviousReturnPage, CorrectionReturnYearPage}
import pages.{EmptyWaypoints, SalesToCountryPage, SoldGoodsPage, SoldToCountryPage, VatOnSalesPage, VatRatesFromCountryPage, Waypoints}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.domain.Vrn

import java.time.*

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with UserAnswersGenerator
    with Generators {

  val userAnswersId: String = "12345-credId"

  val iossEnrolmentKey = "HMRC-IOSS-ORG"
  val enrolments: Enrolments = Enrolments(Set(Enrolment(iossEnrolmentKey, Seq.empty, "test", None)))
  val testCredentials: Credentials = Credentials(userAnswersId, "GGW")
  val vrn: Vrn = Vrn("123456789")
  val iossNumber: String = "IM9001234567"
  val period: StandardPeriod = StandardPeriod(2024, Month.MARCH)
  val waypoints: Waypoints = EmptyWaypoints
  val index: Index = Index(0)
  val vatRateIndex: Index = Index(0)
  val vatRates = List(VatRateFromCountry(BigDecimal(20), VatRateType.Standard, LocalDate.of(2023, 3, 1), None))
  val twentyPercentVatRate = VatRateFromCountry(20, VatRateType.Reduced, arbitrary[LocalDate].sample.value)
  val fivePercentVatRate = VatRateFromCountry(5, VatRateType.Reduced, arbitrary[LocalDate].sample.value)

  def commencementDate: LocalDate = period.firstDay.minusDays(1)

  lazy val registrationWrapper: RegistrationWrapper = {
    val arbirtyRegistration = Arbitrary.arbitrary[RegistrationWrapper].sample.value
    arbirtyRegistration.copy(registration = arbirtyRegistration.registration.copy(
      schemeDetails = arbirtyRegistration.registration.schemeDetails.copy(commencementDate = commencementDate),
      exclusions = Seq.empty
    ))
  }

  val arbitraryDate: LocalDate = datesBetween(LocalDate.of(2023, 3, 1), LocalDate.of(2025, 12, 31)).sample.value
  val arbitraryInstant: Instant = arbitraryDate.atStartOfDay(ZoneId.systemDefault).toInstant
  val stubClockAtArbitraryDate: Clock = Clock.fixed(arbitraryInstant, ZoneId.systemDefault)

  def emptyUserAnswers: UserAnswers = UserAnswers(userAnswersId, period, lastUpdated = arbitraryInstant)

  def completeUserAnswers: UserAnswers = emptyUserAnswers
    .set(SoldGoodsPage, true).success.value
    .set(SoldToCountryPage(index), Country("HR", "Croatia")).success.value
    .set(VatRatesFromCountryPage(index, index), vatRates).success.value
    .set(SalesToCountryPage(index, index), BigDecimal(100)).success.value
    .set(VatOnSalesPage(index, index), VatOnSales(VatOnSalesChoice.Standard, BigDecimal(20))).success.value

  val completedUserAnswersWithCorrections: UserAnswers = completeUserAnswers
    .set(CorrectPreviousReturnPage(0), true).success.value
    .set(CorrectionReturnYearPage(index), 2023).success.value

  val vatCustomerInfo: VatCustomerInfo =
    VatCustomerInfo(
      registrationDate = Some(LocalDate.now(stubClockAtArbitraryDate)),
      desAddress = DesAddress("Line 1", None, None, None, None, Some("AA11 1AA"), "GB"),
      partOfVatGroup = false,
      organisationName = Some("Company name"),
      singleMarketIndicator = true,
      individualName = None,
      deregistrationDecisionDate = Some(LocalDate.now(stubClockAtArbitraryDate)),
      overseasIndicator = false
    )

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(
                                    userAnswers: Option[UserAnswers] = None,
                                    clock: Option[Clock] = None,
                                    registration: RegistrationWrapper = registrationWrapper,
                                    getRegistrationAction: Option[GetRegistrationAction] = None
                                  ): GuiceApplicationBuilder = {
    val clockToBind = clock.getOrElse(stubClockAtArbitraryDate)

    val getRegistrationActionBind = if(getRegistrationAction.nonEmpty) {
      bind[GetRegistrationAction].toInstance(getRegistrationAction.get)
    } else {
      bind[GetRegistrationAction].toInstance(new FakeGetRegistrationAction(registration))
    }

    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[Clock].toInstance(clockToBind),
        bind[DataRetrievalActionProvider].toInstance(new FakeDataRetrievalActionProvider(userAnswers)),
        getRegistrationActionBind,
        bind[CheckBouncedEmailFilterProvider].toInstance(new FakeCheckBouncedEmailFilterProvider()),
        bind[CheckSubmittedReturnsFilterProvider].toInstance(new FakeCheckSubmittedReturnsFilterProvider())
      )
  }

  val coreVatReturn: CoreVatReturn = CoreVatReturn(
    vatReturnReferenceNumber = "XI/XI063407423/M11.2086",
    version = Instant.ofEpochSecond(1630670836),
    traderId = CoreTraderId(vrn.vrn, "XI"),
    period = CorePeriod(2021, "03"),
    startDate = LocalDate.now(stubClockAtArbitraryDate),
    endDate = LocalDate.now(stubClockAtArbitraryDate),
    submissionDateTime = Instant.now(stubClockAtArbitraryDate),
    totalAmountVatDueGBP = BigDecimal(10),
    msconSupplies = List(CoreMsconSupply(
      "DE",
      BigDecimal(10),
      BigDecimal(10),
      BigDecimal(-10),
      List(CoreSupply(
        "GOODS",
        BigDecimal(10),
        "STANDARD",
        BigDecimal(10),
        BigDecimal(10)
      )),
      List(CoreCorrection(
        CorePeriod(2021, "02"),
        BigDecimal(-10)
      ))
    )),
    changeDate = Some(LocalDateTime.now(stubClockAtArbitraryDate))
  )
}
