package repositories

import config.FrontendAppConfig
import models.Period
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.PlayMongoRepositorySupport
import viewmodels.previousReturns.{PreviousRegistration, SelectedPreviousRegistration}

import java.time.YearMonth
import scala.concurrent.ExecutionContext.Implicits.global

class SelectedPreviousRegistrationRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with PlayMongoRepositorySupport[SelectedPreviousRegistration]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  val previousRegistrationIM900987654322: PreviousRegistration = PreviousRegistration(
    "IM900987654322",
    Period(YearMonth.of(2021, 3)),
    Period(YearMonth.of(2021, 10))
  )

  private val selectedPreviousRegistration: SelectedPreviousRegistration = SelectedPreviousRegistration("id", previousRegistrationIM900987654322)

  private val mockAppConfig = mock[FrontendAppConfig]

  protected override val repository = new SelectedPreviousRegistrationRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig
  )

  ".set" - {

    "must save data" in {
      val setResult     = repository.set(selectedPreviousRegistration).futureValue
      val updatedRecord = find(Filters.equal("userId", selectedPreviousRegistration.userId)).futureValue.headOption.value

      setResult mustEqual selectedPreviousRegistration
      updatedRecord mustEqual selectedPreviousRegistration
    }
  }

  ".get" - {

    "must return saved record when one exists for this user id" in {

      repository.set(selectedPreviousRegistration).futureValue

      val result = repository.get(selectedPreviousRegistration.userId).futureValue

      result.value mustEqual selectedPreviousRegistration
    }

    "must return None when no data exists" in {

      val result = repository.get("").futureValue

      result must not be defined
    }
  }

  ".clear" - {

    "must remove a record" in {

      repository.set(selectedPreviousRegistration).futureValue

      val result = repository.clear(selectedPreviousRegistration.userId).futureValue

      result mustEqual true
      repository.get(selectedPreviousRegistration.userId).futureValue must not be defined
    }
  }
}

