package repositories

import config.FrontendAppConfig
import models.IntermediarySelectedIossNumber
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global

class IntermediarySelectedIossNumberRepositorySpec extends AnyFreeSpec
  with Matchers
  with DefaultPlayMongoRepositorySupport[IntermediarySelectedIossNumber]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val iossNumber: String = arbitrary[String].sample.value
  private val intermediaryNumber: String = arbitrary[String].sample.value
  private val intermediarySelectedIossNumber: IntermediarySelectedIossNumber = IntermediarySelectedIossNumber("userId", intermediaryNumber, iossNumber, lastUpdated = Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  protected override val repository: IntermediarySelectedIossNumberRepository = new IntermediarySelectedIossNumberRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  ".set" - {

    "must set the last updated time on the iossNumber to `now`, and save them" in {

      val expectedResult = intermediarySelectedIossNumber.copy(lastUpdated = instant)

      val setResult = repository.set(intermediarySelectedIossNumber).futureValue
      val updatedRecord = find(Filters.equal("userId", intermediarySelectedIossNumber.userId)).futureValue.headOption.value

      setResult mustBe expectedResult
      updatedRecord mustBe expectedResult
    }
  }

  ".get" - {

    "must return saved record when one exists for this user id" in {

      insert(intermediarySelectedIossNumber).futureValue

      val result = repository.get(intermediarySelectedIossNumber.userId).futureValue
      val expectedResult = intermediarySelectedIossNumber

      result.value mustBe expectedResult
    }

    "must return None when no data exists" in {

      val result = repository.get("userid that does not exist").futureValue

      result must not be defined
    }
  }

  ".clear" - {

    "must remove a record and return true when there is a record for this userid" in {

      insert(intermediarySelectedIossNumber).futureValue

      val result = repository.clear(intermediarySelectedIossNumber.userId).futureValue

      result mustBe true
      repository.get(intermediarySelectedIossNumber.userId).futureValue must not be defined
    }

    "must return true when there is no record for this userid" in {

      val result = repository.clear("userid that does not exist").futureValue

      result mustBe true
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(intermediarySelectedIossNumber).futureValue

        val result = repository.keepAlive(intermediarySelectedIossNumber.userId).futureValue

        val expectedUpdatedAnswers = intermediarySelectedIossNumber copy (lastUpdated = instant)

        result mustEqual true
        val updatedAnswers = find(Filters.equal("userId", intermediarySelectedIossNumber.userId)).futureValue.headOption.value
        updatedAnswers mustEqual expectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist").futureValue mustEqual true
      }
    }
  }
}
