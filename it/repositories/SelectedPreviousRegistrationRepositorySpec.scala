package repositories

import config.FrontendAppConfig
import models.StandardPeriod
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import viewmodels.previousReturns.{PreviousRegistration, SelectedPreviousRegistration}

import java.time.{Clock, Instant, YearMonth, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class SelectedPreviousRegistrationRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[SelectedPreviousRegistration]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  val previousRegistrationIM900987654322: PreviousRegistration = PreviousRegistration(
    "IM900987654322",
    StandardPeriod(YearMonth.of(2021, 3)),
    StandardPeriod(YearMonth.of(2021, 10))
  )

  private val selectedPreviousRegistration: SelectedPreviousRegistration = SelectedPreviousRegistration("id", previousRegistrationIM900987654322, lastUpdated = Instant.now(stubClock))

  private val mockAppConfig = mock[FrontendAppConfig]

  protected override val repository = new SelectedPreviousRegistrationRepository(
    mongoComponent = mongoComponent,
    appConfig      = mockAppConfig,
    clock          = stubClock
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

