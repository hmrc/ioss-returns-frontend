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

package controllers.corrections

import base.SpecBase
import connectors.VatReturnConnector
import models.{Country, Index, Period, UserAnswers}
import models.etmp.{EtmpObligation, EtmpObligationDetails, EtmpObligations, EtmpObligationsFulfilmentStatus}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.EmptyWaypoints
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import views.html.corrections.VatPeriodCorrectionsListView

import java.time.Month
import scala.concurrent.Future

class VatPeriodCorrectionsListControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {
  private val year = 2021
  private val periodJuly2021 = Period(year, Month.JULY)

  private lazy val vatPeriodCorrectionsListRoute = controllers
    .corrections.routes.VatPeriodCorrectionsListController.onPageLoad(EmptyWaypoints, periodJuly2021).url

  private lazy val vatPeriodCorrectionsListRoutePost = controllers
    .corrections.routes.VatPeriodCorrectionsListController.onSubmit(EmptyWaypoints, periodJuly2021, false).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[Period]): Option[UserAnswers] = //Some(userAnswers)
    periods.zipWithIndex
      .foldLeft(Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), (indexedPeriod._1)).toOption)
          .flatMap(_.set(CorrectionCountryPage(Index(indexedPeriod._2), Index(0)), Country.euCountries.head).toOption)
          .flatMap(_.set(VatAmountCorrectionCountryPage(Index(indexedPeriod._2), Index(0)), BigDecimal(200.0)).toOption))

  private def getStatusResponse(periods: Seq[Period]): Future[EtmpObligations] = {
    Future.successful {

      val details = periods.map(period => EtmpObligationDetails(EtmpObligationsFulfilmentStatus.Fulfilled, period.toEtmpPeriodString))

      EtmpObligations(obligations = Seq(EtmpObligation(details)))
    }
  }

  private val mockVatReturnConnector = mock[VatReturnConnector]

  private def vatCorrectionsListUrl(index: Int) = s"/pay-vat-on-goods-sold-to-eu/import-one-stop-shop-returns-payments/correction-list-countries/$index"

  private def removePeriodCorrectionUrl(index: Int) = s"/pay-vat-on-goods-sold-to-eu/import-one-stop-shop-returns-payments/remove-period-correction/$index"

  override def completeUserAnswers: UserAnswers = emptyUserAnswers.copy(period = periodJuly2021)

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockVatReturnConnector)
  }

  "VatPeriodCorrectionsList Controller" - {
    val allPeriods = Seq(
      periodJuly2021,
      Period(year, Month.OCTOBER),
      Period(year + 1, Month.JANUARY)
    )
    val allPeriodsModel = Seq(
      ListItem(
        name = "1 July to 31 July 2021",
        changeUrl = vatCorrectionsListUrl(1),
        removeUrl = removePeriodCorrectionUrl(1)
      ),
      ListItem(
        name = "1 October to 31 October 2021",
        changeUrl = vatCorrectionsListUrl(2),
        removeUrl = removePeriodCorrectionUrl(2)
      ),
      ListItem(
        name = "1 January to 31 January 2022",
        changeUrl = vatCorrectionsListUrl(3),
        removeUrl = removePeriodCorrectionUrl(3)
      )
    )

    "when there are no previous return periods must redirect to JourneyRecovery" in {

      when(mockVatReturnConnector.getObligations(any())(any()))
        .thenReturn(Future.successful(EtmpObligations(obligations = Seq(EtmpObligation(Nil)))))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .configure("bootstrap.filters.csrf.enabled" -> false)
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are previous return periods" - {

      "and there are uncompleted correction periods must redirect to page with form" in {

        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val completedCorrections = Seq()

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
          val result = route(application, request).value
          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual
            controllers.corrections.routes.VatPeriodCorrectionsListWithFormController
              .onPageLoad(EmptyWaypoints, periodJuly2021).url
        }
      }

      "and there no uncompleted correction periods must display filled table and correct header" in {

        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val expectedTitle = "You have corrected the VAT amount for 3 return periods"
        val expectedTableRows = 3
        val userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods)
        val application = applicationBuilder(userAnswers) //addCorrectionPeriods(completeUserAnswers, allPeriods))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          val responseString = contentAsString(result)

          val doc = Jsoup.parse(responseString)
          doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
          doc.getElementsByClass("hmrc-add-to-a-list__contents").size() mustEqual expectedTableRows

          val view = application.injector.instanceOf[VatPeriodCorrectionsListView]

          responseString.filterNot(_.isWhitespace) mustEqual
            view(EmptyWaypoints, periodJuly2021, allPeriodsModel, List.empty)(request, messages(application))
              .toString.filterNot(_.isWhitespace)
        }
      }

      "and there no uncompleted correction periods must display filled table and correct header and missing data warning" in {

        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val expectedTitle = "You have corrected the VAT amount for 3 return periods"
        val expectedTableRows = 3
        val answers = addCorrectionPeriods(completeUserAnswers, allPeriods.tail).value
          .set(CorrectionReturnPeriodPage(Index(allPeriods.tail.size)), allPeriods.head).success.value
          .set(CorrectionCountryPage(Index(allPeriods.tail.size), index), Country.euCountries.head).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

          val result = route(application, request).value

          status(result) mustEqual OK
          val responseString = contentAsString(result)

          val doc = Jsoup.parse(responseString)
          doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
          doc.getElementsByClass("hmrc-add-to-a-list__contents").size() mustEqual expectedTableRows

          val view = application.injector.instanceOf[VatPeriodCorrectionsListView]
          responseString.filterNot(_.isWhitespace) mustEqual
            view(waypoints, periodJuly2021, allPeriodsModel, List(allPeriods.head))(request, messages(application))
              .toString.filterNot(_.isWhitespace)
        }
      }
    }

    "POST" - {
      "must redirect to VatCorrectionsList if there are correction amounts missing for a period" in {
        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val answers = addCorrectionPeriods(completeUserAnswers, allPeriods.tail).value
          .set(CorrectionReturnPeriodPage(Index(allPeriods.tail.size)), allPeriods.head).success.value
          .set(CorrectionCountryPage(Index(allPeriods.tail.size), index), Country.euCountries.head).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListController.onSubmit(waypoints, period, true).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          redirectLocation(result).value mustEqual
            controllers.corrections.routes.CorrectionListCountriesController
              .onPageLoad(waypoints, Index(allPeriods.size - 1)).url
        }
      }

      "must refresh the page if there are correction amounts missing for a period" in {
        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val answers = addCorrectionPeriods(completeUserAnswers, allPeriods.tail).value
          .set(CorrectionReturnPeriodPage(Index(allPeriods.tail.size)), allPeriods.head).success.value
          .set(CorrectionCountryPage(Index(allPeriods.tail.size), index), Country.euCountries.head).success.value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListController.onSubmit(waypoints, period, false).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, period).url
        }
      }

      "must redirect to CheckYourAnswers" in {
        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        val answers = addCorrectionPeriods(completeUserAnswers, allPeriods).value

        val application = applicationBuilder(userAnswers = Some(answers))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        running(application) {
          val request = FakeRequest(POST, vatPeriodCorrectionsListRoutePost)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.CheckYourAnswersController.onPageLoad().url
        }
      }
    }
  }
}

