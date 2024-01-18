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
import forms.corrections.VatPeriodCorrectionsListFormProvider
import models.etmp.{EtmpObligation, EtmpObligationDetails, EtmpObligations, EtmpObligationsFulfilmentStatus}
import models.{Country, Index, Period, UserAnswers}
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, VatAmountCorrectionCountryPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.addtoalist.ListItem
import views.html.corrections.VatPeriodAvailableCorrectionsListView

import java.time.Month
import scala.concurrent.Future

class VatPeriodCorrectionsListWithFormControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private lazy val vatPeriodCorrectionsListRoute = controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onPageLoad(waypoints, periodJuly2021).url

  private def addCorrectionPeriods(userAnswers: UserAnswers, periods: Seq[Period]): Option[UserAnswers] = //Some(userAnswers)
    periods.zipWithIndex
      .foldLeft(Option(userAnswers))((ua, indexedPeriod) =>
        ua.flatMap(_.set(CorrectionReturnPeriodPage(Index(indexedPeriod._2)), (indexedPeriod._1)).toOption)
          .flatMap(_.set(CorrectionCountryPage(Index(indexedPeriod._2), Index(0)), Country.euCountries.head).toOption)
          .flatMap(_.set(VatAmountCorrectionCountryPage(Index(indexedPeriod._2), Index(0)), BigDecimal(200.0)).toOption))

  private def getStatusResponse(periods: Seq[Period], status: EtmpObligationsFulfilmentStatus = EtmpObligationsFulfilmentStatus.Fulfilled): Future[EtmpObligations] = {
    Future.successful {

      val details = periods.map(p => EtmpObligationDetails(status, p.toEtmpPeriodString)) //Period//PeriodWithStatus(period, Complete))))
      EtmpObligations(obligations = Seq(EtmpObligation("", "", details)))
    }
  }

  private val mockVatReturnConnector = mock[VatReturnConnector]

  private val formProvider = new VatPeriodCorrectionsListFormProvider()
  private val form = formProvider()

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(mockVatReturnConnector)
  }

  val year = 2021
  val periodJuly2021 = Period(year, Month.JULY)

  override def completeUserAnswers: UserAnswers = emptyUserAnswers.copy(period = periodJuly2021)

  val allPeriods = Seq(
    periodJuly2021,
    Period(year, Month.OCTOBER),
    Period(year + 1, Month.JANUARY)
  )

  "VatPeriodCorrectionsListWithFormController" - {

    "must throw an exception when Return Status Connector returns an error" in {
      val exceptionMessage = "some error"
      when(mockVatReturnConnector.getObligations(any())(any())) thenReturn Future.failed(new Exception(exceptionMessage))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
        val result = route(application, request).value

        whenReady(result.failed) { exp =>

          exp.getMessage mustEqual exceptionMessage
        }
      }
    }

    "when there are no previous return periods, it must redirect to JourneyRecovery" in {

      when(mockVatReturnConnector.getObligations(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, EtmpObligationsFulfilmentStatus.Open))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "when there are previous return periods" - {

      "and no corrections have been added must display empty table and form" in {

        val expectedTitle = "You have not corrected the VAT amount for any return periods"
        val expectedTableRows = 0

        val completedCorrections = List.empty[Period]
        val completedCorrectionsModel = Seq.empty[ListItem]

        val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, completedCorrections))
          .configure("bootstrap.filters.csrf.enabled" -> false)
          .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
          .build()

        when(mockVatReturnConnector.getObligations(any())(any()))
          .thenReturn(getStatusResponse(allPeriods))

        running(application) {
          val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
          val result = route(application, request).value

          status(result) mustEqual OK
          val responseString = contentAsString(result)
          val doc = Jsoup.parse(responseString)

          doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
          doc.getElementsByClass("govuk-table__row").size() mustBe expectedTableRows

          val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]
          responseString mustEqual
            view(form, waypoints, periodJuly2021, completedCorrectionsModel, List.empty)(request, messages(application)).toString
        }
      }

      "and corrections have been added" - {

        "and there are uncompleted correction periods must display filled table and form" in {

          val expectedTitle = "You have corrected the VAT amount for one return period"
          val expectedTableRows = 1
          val periodQ3 = periodJuly2021

          val completedCorrections = List(periodQ3)

          val completedCorrectionsModel = Seq(
            ListItem(
              name = "1 July to 31 July 2021",
              changeUrl = controllers.corrections.routes.CorrectionListCountriesController.onPageLoad(waypoints, index).url,
              removeUrl = controllers.corrections.routes.RemovePeriodCorrectionController.onPageLoad(waypoints, index).url
            )
          )

          val ua = addCorrectionPeriods(completeUserAnswers, completedCorrections)

          val application = applicationBuilder(userAnswers = ua)
            .configure("bootstrap.filters.csrf.enabled" -> false)
            .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
            .build()

          when(mockVatReturnConnector.getObligations(any())(any()))
            .thenReturn(getStatusResponse(allPeriods))

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)
            val result = route(application, request).value
            status(result) mustEqual OK

            val responseString = contentAsString(result)
            val doc = Jsoup.parse(responseString)

            doc.getElementsByClass("govuk-heading-xl").get(0).text() mustEqual expectedTitle
            doc.getElementsByClass("hmrc-add-to-a-list__contents").size() mustEqual expectedTableRows

            val view = application.injector.instanceOf[VatPeriodAvailableCorrectionsListView]

            responseString mustEqual
              view(form, waypoints, periodJuly2021, completedCorrectionsModel, List.empty)(request, messages(application)).toString
          }
        }

        "and there are no uncompleted correction periods must redirect to page without form" in {

          when(mockVatReturnConnector.getObligations(any())(any()))
            .thenReturn(getStatusResponse(allPeriods))

          val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
            .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
            .build()

          running(application) {
            val request = FakeRequest(GET, vatPeriodCorrectionsListRoute)

            val result = route(application, request).value
            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, periodJuly2021).url
          }
        }
      }
    }

    "and there are no uncompleted correction periods must redirect to page without form for a POST" in {

      when(mockVatReturnConnector.getObligations(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, EtmpObligationsFulfilmentStatus.Fulfilled))

      val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST,
          controllers.corrections.routes.VatPeriodCorrectionsListWithFormController
            .onSubmit(waypoints, periodJuly2021, false).url)

        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.corrections.routes.VatPeriodCorrectionsListController.onPageLoad(waypoints, periodJuly2021).url
      }
    }

    "when there are no previous return periods must redirect to JourneyRecovery for a POST" in {

      when(mockVatReturnConnector.getObligations(any())(any()))
        .thenReturn(getStatusResponse(allPeriods, EtmpObligationsFulfilmentStatus.Open))

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        implicit val request = FakeRequest(POST,
          controllers.corrections.routes.VatPeriodCorrectionsListWithFormController
            .onSubmit(waypoints, periodJuly2021, false).url)
        val result = route(application, request).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must throw an exception when the Return Status Connector returns an Unexpected Response" in {
      val exceptionMessage = "some error"
      when(mockVatReturnConnector.getObligations(any())(any())) thenReturn Future.failed(new Exception(exceptionMessage))

      val application = applicationBuilder(userAnswers = addCorrectionPeriods(completeUserAnswers, allPeriods))
        .overrides(bind[VatReturnConnector].toInstance(mockVatReturnConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.corrections.routes.VatPeriodCorrectionsListWithFormController.onSubmit(waypoints, period, false).url)

        val result = route(application, request).value

        whenReady(result.failed) {
          exp => exp.getMessage mustEqual (exceptionMessage)
        }
      }
    }
  }
}
