package repositories

import org.mockito.MockitoSugar
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}
import viewmodels.previousReturns.PreviousRegistration

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationStatusRepositorySpec extends AnyFreeSpec
  with Matchers
  with CleanMongoCollectionSupport
  with PlayMongoRepositorySupport[PreviousRegistration]
  with ScalaFutures
  with IntegrationPatience
  with OptionValues
  with MockitoSugar {

  private val appConfig = mock[AppConfig]
  protected val repository =
    new SelectedPreviousRegistrationRepository(
      mongoComponent = mongoComponent,
      appConfig = appConfig
    )

  val registrationStatus: RegistrationStatus = RegistrationStatus("100000001-id",
    EtmpRegistrationStatus.Success)

  ".insert" - {

    "must insert data" in {

      val insertResult = repository.insert(registrationStatus).futureValue
      val databaseRecord = findAll().futureValue.headOption.value

      insertResult mustEqual InsertSucceeded
      databaseRecord mustEqual registrationStatus
    }

    "must not allow duplicate data to be inserted" in {

      repository.insert(registrationStatus).futureValue
      val secondResult = repository.insert(registrationStatus).futureValue

      secondResult mustEqual AlreadyExists
    }
  }

  ".set registration status" - {

    "must update the existing status" in {

      repository.insert(registrationStatus).futureValue
      val updatedRegistrationStatus = registrationStatus.copy(status = EtmpRegistrationStatus.Pending)

      val updatedResult = repository.set(updatedRegistrationStatus).futureValue

      updatedResult mustEqual updatedRegistrationStatus
    }
  }

  ".get one" - {

    "must return Saved record when one exists for this subscription id" in {

      insert(registrationStatus).futureValue

      val result = repository.get(registrationStatus.subscriptionId).futureValue

      result.value mustEqual registrationStatus
    }

    "must return None when no data exists" in {

      val result = repository.get("100000002-id").futureValue

      result must not be defined
    }
  }

  ".delete" - {

    "must return true when saved record is deleted" in {

      insert(registrationStatus).futureValue

      val result = repository.delete(registrationStatus.subscriptionId).futureValue

      result mustEqual true

      val currentAnswers = repository.get(registrationStatus.subscriptionId).futureValue
      currentAnswers must not be defined
    }
  }
}
