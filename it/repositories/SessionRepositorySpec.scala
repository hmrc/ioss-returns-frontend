package repositories

import com.typesafe.config.Config
import config.FrontendAppConfig
import crypto.UserAnswersEncryptor
import models.{EncryptedUserAnswers, Period, StandardPeriod, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mongodb.scala.model.Filters
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.Configuration
import play.api.libs.json.Json
import services.crypto.EncryptionService
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, Month, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext.Implicits.global

class SessionRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[EncryptedUserAnswers]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)
  private val period: Period = StandardPeriod(2024, Month.MARCH)

  private val userAnswers = UserAnswers("id", "IM9001234567", period, Json.obj("foo" -> "bar"), Instant.ofEpochSecond(1))

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1L

  private val mockConfiguration = mock[Configuration]
  private val mockConfig = mock[Config]
  private val mockEncryptionService: EncryptionService = new EncryptionService(mockConfiguration)
  private val encryptor = new UserAnswersEncryptor(mockAppConfig, mockEncryptionService)
  private val secretKey: String = "VqmXp7yigDFxbCUdDdNZVIvbW6RgPNJsliv6swQNCL8="

  protected override val repository: SessionRepository = new SessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    encryptor = encryptor,
    clock = stubClock
  )

  when(mockConfiguration.underlying) thenReturn mockConfig
  when(mockConfig.getString(any())) thenReturn secretKey
  when(mockAppConfig.encryptionKey) thenReturn secretKey

  ".set" - {

    "must set the last updated time on the supplied user answers to `now`, and save them" in {

      val expectedResult = userAnswers copy (lastUpdated = instant)
      val encryptedExpectedResult = encryptor.encryptUserAnswers(expectedResult)

      val setResult     = repository.set(userAnswers).futureValue
      val updatedRecord = find(Filters.equal("userId", userAnswers.userId)).futureValue.headOption.value

      setResult mustEqual true
      updatedRecord mustEqual encryptedExpectedResult
    }
  }

  ".get" - {

    "when there is a record for this id" - {

      "must update the lastUpdated time and get the record" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

        val result         = repository.get(userAnswers.userId, userAnswers.iossNumber).futureValue
        val expectedResult = userAnswers copy (lastUpdated = instant)

        result.value mustEqual expectedResult
      }
    }

    "when there is no record for this id" - {

      "must return None" in {

        repository.get("id that does not exist", userAnswers.iossNumber).futureValue must not be defined
        repository.get(userAnswers.userId, "id that does not exist").futureValue must not be defined
      }
    }
  }

  ".clear" - {

    "must remove a record" in {

      insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

      val result = repository.clear(userAnswers.userId, userAnswers.iossNumber).futureValue

      result mustEqual true
      repository.get(userAnswers.userId, userAnswers.iossNumber).futureValue must not be defined
    }

    "must return true when there is no record to remove" in {
      val result = repository.clear("id that does not exist", userAnswers.iossNumber).futureValue
      val result2 = repository.clear(userAnswers.userId, "id that does not exist").futureValue

      result mustEqual true
      result2 mustEqual true
    }
  }

  ".keepAlive" - {

    "when there is a record for this id" - {

      "must update its lastUpdated to `now` and return true" in {

        insert(encryptor.encryptUserAnswers(userAnswers)).futureValue

        val result = repository.keepAlive(userAnswers.userId, userAnswers.iossNumber).futureValue

        val expectedUpdatedAnswers = userAnswers copy (lastUpdated = instant)
        val encryptedExpectedUpdatedAnswers = encryptor.encryptUserAnswers(expectedUpdatedAnswers)

        result mustEqual true
        val updatedAnswers = find(Filters.equal("userId", userAnswers.userId)).futureValue.headOption.value
        updatedAnswers mustEqual encryptedExpectedUpdatedAnswers
      }
    }

    "when there is no record for this id" - {

      "must return true" in {

        repository.keepAlive("id that does not exist", userAnswers.iossNumber).futureValue mustEqual true
        repository.keepAlive(userAnswers.userId, "id that does not exist").futureValue mustEqual true
      }
    }
  }
}
