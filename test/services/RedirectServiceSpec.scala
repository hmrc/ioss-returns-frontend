/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase
import cats.implicits.catsSyntaxValidatedIdBinCompat0
import controllers.actions.AuthenticatedControllerComponents
import controllers.corrections.routes as correctionRoutes
import controllers.routes
import models.requests.DataRequest
import models.{DataMissingError, Index, UserAnswers, ValidationError}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.corrections.{CorrectionCountryPage, CorrectionReturnPeriodPage, CorrectionReturnYearPage}
import pages.{SalesToCountryPage, SoldGoodsPage, VatOnSalesPage, VatRatesFromCountryPage}
import play.api.inject.bind
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.*

class RedirectServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockVatReturnService: VatReturnService = mock[VatReturnService]
  private val mockCorrectionService: CorrectionService = mock[CorrectionService]

  private implicit val dataRequest: DataRequest[AnyContent] = DataRequest[AnyContent](FakeRequest(), testCredentials, vrn, iossNumber, registrationWrapper, completedUserAnswersWithCorrections)

  override def beforeEach(): Unit = {
    reset(mockVatReturnService)
    reset(mockCorrectionService)
    super.beforeEach()
  }

  "RedirectService" - {

    ".validate" - {

      "must return a list of VAT Return validation errors when users answers contains validation errors for a VAT Return" in {

        val validationError: ValidationError = DataMissingError(SalesToCountryPage(Index(0), Index(0)))

        when(mockVatReturnService.fromUserAnswers(any(), any(), any())) thenReturn validationError.invalidNec
        when(mockCorrectionService.fromUserAnswers(any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val result = service.validate(period)

          result `mustBe` List(validationError)
        }
      }

      "must return a list of Correction validation errors when users answers contains validation errors for Corrections" in {

        val validationError: ValidationError = DataMissingError(CorrectionCountryPage(Index(0), Index(0)))

        when(mockCorrectionService.fromUserAnswers(any(), any(), any())) thenReturn validationError.invalidNec
        when(mockVatReturnService.fromUserAnswers(any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val result = service.validate(period)

          result `mustBe` List(validationError)
        }
      }

      "must return a list of both VAT Return and Correction validation errors when users answers contains validation " +
        "errors for both VAT Return and Corrections" in {

        val vatReturnValidationError: ValidationError = DataMissingError(SalesToCountryPage(Index(0), Index(0)))
        val correctionValidationError: ValidationError = DataMissingError(CorrectionCountryPage(Index(0), Index(0)))

        when(mockVatReturnService.fromUserAnswers(any(), any(), any())) thenReturn vatReturnValidationError.invalidNec
        when(mockCorrectionService.fromUserAnswers(any(), any(), any())) thenReturn correctionValidationError.invalidNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val result = service.validate(period)

          result `mustBe` List(vatReturnValidationError, correctionValidationError)
        }
      }

      "must return an empty list when users answers contain no validation errors for VAT returns or Corrections" in {

        when(mockCorrectionService.fromUserAnswers(any(), any(), any())) thenReturn List.empty.validNec
        when(mockVatReturnService.fromUserAnswers(any(), any(), any())) thenReturn List.empty.validNec

        val application = applicationBuilder()
          .overrides(bind[VatReturnService].toInstance(mockVatReturnService))
          .overrides(bind[CorrectionService].toInstance(mockCorrectionService))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val result = service.validate(period)

          result `mustBe` List.empty
        }
      }
    }

    ".getRedirect" - {

      "must redirect to Sold To Country Controller when there's no data for sales present" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(SalesToCountryPage(Index(0), Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(AllSalesQuery)

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(routes.SoldToCountryController.onPageLoad(waypoints, Index(0)))
        }
      }

      "must redirect to Vat Rates From Country Controller when there's no vat rates data present" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(VatRatesFromCountryPage(Index(0), Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(VatRatesFromCountryPage(Index(0), Index(0)))

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(routes.VatRatesFromCountryController.onPageLoad(waypoints, Index(0)))
        }
      }

      "must redirect to Sales To Country Controller when there's no vat rates data for a selected country present" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(SalesToCountryPage(Index(0), Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(SalesAtVatRateQuery(Index(0), Index(0)))

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(routes.SalesToCountryController.onPageLoad(waypoints, Index(0), Index(0)))
        }
      }

      "must redirect to Vat On Sales Controller when there's no vat charged on sales at vat rate data for a selected country present" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(VatOnSalesPage(Index(0), Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(VatOnSalesFromQuery(Index(0), Index(0)))

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(routes.VatOnSalesController.onPageLoad(waypoints, Index(0), Index(0)))
        }
      }

      "must redirect to Correction Return Year Controller when there's no corrections data present" in {

        val updatedUserAnswers: UserAnswers = completedUserAnswersWithCorrections
          .remove(CorrectionReturnYearPage(Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(AllCorrectionPeriodsQuery)

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(correctionRoutes.CorrectionReturnYearController.onPageLoad(waypoints, Index(0)))
        }
      }

      "must redirect to Correction Country Controller when there's no correction countries for period data present" in {

        val updatedUserAnswers: UserAnswers = completedUserAnswersWithCorrections
          .remove(CorrectionReturnPeriodPage(Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(AllCorrectionCountriesQuery(Index(0)))

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(correctionRoutes.CorrectionCountryController.onPageLoad(waypoints, Index(0), Index(0)))
        }
      }

      "must redirect to Vat Amount Correction Country Controller when there's no correction to country for period data present" in {

        val updatedUserAnswers: UserAnswers = completedUserAnswersWithCorrections
          .remove(CorrectionCountryPage(Index(0), Index(0))).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(CorrectionToCountryQuery(Index(0), Index(0)))

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` Some(correctionRoutes.VatAmountCorrectionCountryController.onPageLoad(waypoints, Index(0), Index(0)))
        }
      }

      "must return None when any other data is missing" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers
          .remove(SoldGoodsPage).success.value

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val validationError = DataMissingError(SoldGoodsPage)

          val result = service.getRedirect(waypoints, List(validationError)).headOption

          result `mustBe` None
        }
      }

      "must return None when there are no validation errors" in {

        val updatedUserAnswers: UserAnswers = completeUserAnswers

        val application = applicationBuilder(userAnswers = Some(updatedUserAnswers))
          .build()

        running(application) {

          val cc = application.injector.instanceOf(classOf[AuthenticatedControllerComponents])

          val service = new RedirectService(cc, mockCorrectionService, mockVatReturnService)

          val result = service.getRedirect(waypoints, List.empty).headOption

          result `mustBe` None
        }
      }
    }
  }
}