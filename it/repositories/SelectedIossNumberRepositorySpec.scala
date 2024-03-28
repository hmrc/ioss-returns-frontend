package repositories

import config.FrontendAppConfig
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import viewmodels.payments.SelectedIossNumber

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class SelectedIossNumberRepositorySpec extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[SelectedIossNumber]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val iossNumber: String = arbitrary[String].sample.value
  private val selectedIossNumber: SelectedIossNumber = SelectedIossNumber("userId", iossNumber, lastUpdated = Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  protected override val repository: SelectedIossNumberRepository = new SelectedIossNumberRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the iossNumber to `now`, and save them" in {

      val expectedResult = selectedIossNumber.copy(lastUpdated = instant)

      val setResult = repository.set(selectedIossNumber).futureValue
      val updatedRecord = find(Filters.equal("userId", selectedIossNumber.userId)).futureValue.headOption.value

      setResult mustBe expectedResult
      updatedRecord mustBe expectedResult
    }
  }

  ".get" - {

    "must return saved record when one exists for this user id" in {

      insert(selectedIossNumber).futureValue

      val result = repository.get(selectedIossNumber.userId).futureValue
      val expectedResult = selectedIossNumber

      result.value mustBe expectedResult
    }

    "must return None when no data exists" in {

      val result = repository.get("userid that does not exist").futureValue

      result must not be defined
    }
  }

  ".clear" - {

    "must remove a record and return true when there is a record for this userid" in {

      insert(selectedIossNumber).futureValue

      val result = repository.clear(selectedIossNumber.userId).futureValue

      result mustBe true
      repository.get(selectedIossNumber.userId).futureValue must not be defined
    }

    "must return true when there is no record for this userid" in {

      val result = repository.clear("userid that does not exist").futureValue

      result mustBe true
    }
  }
}
