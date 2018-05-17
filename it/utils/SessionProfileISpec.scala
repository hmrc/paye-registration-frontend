
package utils

import connectors.KeystoreConnector
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.external.CurrentProfile
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.Future

class SessionProfileISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-frontend-stubs.host" -> s"$mockHost",
    "microservice.services.incorporation-frontend-stubs.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "mongodb.uri" -> s"$mongoUri"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val sessionId = "session-123"
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  lazy val mockKeystoreConnector = app.injector.instanceOf[KeystoreConnector]

  implicit val fakeRequest = FakeRequest("GET", "/")
  val testFunc: CurrentProfile => Future[Result] = _ => Future.successful(Ok)
  val regId = "12345"

  trait Setup {
    val sessionProfile = new SessionProfile {
      override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
    }
  }

  "withCurrentProfile" should {
    "get a Current Profile" when {
      "SessionRepository has one" in new Setup {

        stubSessionCacheMetadata(sessionId, regId, false)

        val res = await(sessionProfile.withCurrentProfile(testFunc))
        res.header.status mustBe OK
      }

      "in-flight user (not in SessionRepository - in Keystore) saves CurrentProfile into SessionRepository, do II subscription" in new Setup {

        val currentProfile = Json.parse(
          s"""
            |{
            | "id" : "xxx",
            | "data" : {
            | "CurrentProfile" : {
            |  "registrationID": "$regId",
            |  "companyTaxRegistration": {
            |     "status": "submitted",
            |     "transactionId": "12345"
            |  },
            |  "language": "ENG",
            |  "payeRegistrationSubmitted": false
            |  }
            | }
            |}""".stripMargin).toString()
        stubGet(s"/keystore/paye-registration-frontend/$sessionId", 200, currentProfile)

        val res = await(sessionProfile.withCurrentProfile(testFunc))
        res.header.status mustBe OK
      }
    }

    "redirect to Start PAYE page" when {
      "there is no Current Profile in SessionRepository and Keystore" in new Setup {
        stubGet(s"/keystore/paye-registration-frontend/$sessionId", 404, "")

        val res = await(sessionProfile.withCurrentProfile(testFunc))
        res.header.status mustBe SEE_OTHER
        res.header.headers.get(HeaderNames.LOCATION) mustBe Some(controllers.userJourney.routes.PayeStartController.startPaye().url)
      }
    }
  }
}
