package controllers.actions

import config.FrontendAppConfig
import connectors.RegistrationConnector
import models.RegistrationWrapper
import models.requests.{IdentifierRequest, RegistrationRequest}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.Result
import services.AccountService
import testUtils.RegistrationData.iossEnrolmentKey
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import utils.FutureSyntax.FutureOps

import scala.concurrent.{ExecutionContext, Future}

class FakeGetRegistrationWithoutUrlIossAction(
                                               registrationWrapper: RegistrationWrapper,
                                               maybeIntermediaryNumber: Option[String] = None,
                                               enrolments: Option[Enrolments] = None,
                                               requestedIossNumber: Option[String] = None
                                             )
  extends GetRegistrationWithoutUrlIossAction(
    mock[RegistrationConnector],
    mock[AccountService],
    mock[FrontendAppConfig]
  )(ExecutionContext.Implicits.global) {

  private val iossEnrolment: Enrolments = Enrolments(Set(Enrolment(iossEnrolmentKey, Seq.empty, "test", None)))

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, RegistrationRequest[A]]] = {
    Right(
      RegistrationRequest(
        request = request.request,
        credentials = request.credentials,
        vrn = Some(request.vrn),
        companyName = "Company Name",
        iossNumber = requestedIossNumber.getOrElse("IM9001234567"),
        registrationWrapper = registrationWrapper,
        intermediaryNumber = maybeIntermediaryNumber,
        enrolments = enrolments.getOrElse(iossEnrolment)
      )
    ).toFuture
  }
}
