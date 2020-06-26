/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import com.github.tomakehurst.wiremock.client.WireMock.{deleteRequestedFor, findAll, urlMatching}
import connectors.{IncorporationInformationConnector, KeystoreConnector}
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
import services.PAYERegistrationService
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
    "microservice.services.cachable.short-lived-cache.host" -> s"$mockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$mockPort",
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
  lazy val mockIncorporationInformationConnector = app.injector.instanceOf[IncorporationInformationConnector]
  lazy val mockPayeRegistrationService = app.injector.instanceOf[PAYERegistrationService]

  implicit val fakeRequest = FakeRequest("GET", "/")
  val testFunc: CurrentProfile => Future[Result] = _ => Future.successful(Ok)
  val regId = "12345"

  trait Setup {
    val sessionProfile = new SessionProfile {
      override val keystoreConnector: KeystoreConnector = mockKeystoreConnector
      override val incorporationInformationConnector: IncorporationInformationConnector = mockIncorporationInformationConnector
      override val payeRegistrationService: PAYERegistrationService = mockPayeRegistrationService
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
        stubPost(s"/incorporation-information/subscribe/$regId/regime/paye-fe/subscriber/SCRS", 202, "")
        val res = await(sessionProfile.withCurrentProfile(testFunc))
        res.header.status mustBe OK
      }

      "in-flight user (not in SessionRepository - in Keystore) saves CurrentProfile into SessionRepository, do II subscription returns 200 rejected" in new Setup {
        val txId = "12345"
        val currentProfile = Json.parse(
          s"""
             |{
             | "id" : "xxx",
             | "data" : {
             | "CurrentProfile" : {
             |  "registrationID": "$regId",
             |  "companyTaxRegistration": {
             |     "status": "submitted",
             |     "transactionId": "$txId"
             |  },
             |  "language": "ENG",
             |  "payeRegistrationSubmitted": false
             |  }
             | }
             |}""".stripMargin).toString()

        val responseJson = Json.parse(
          s"""
             |{
             | "SCRSIncorpStatus": {
             |   "IncorpSubscriptionKey" : {
             |     "transactionId" : "$txId",
             |     "subscriber"    : "SCRS",
             |     "discriminator" : "paye-fe"
             |   },
             |   "IncorpStatusEvent": {
             |     "status" : "rejected",
             |     "crn" : "12345678",
             |     "description" : "test desc"
             |   }
             | }
             |}
      """.stripMargin).toString

        stubGet(s"/keystore/paye-registration-frontend/$sessionId", 200, currentProfile)
        stubPost(s"/incorporation-information/subscribe/$regId/regime/paye-fe/subscriber/SCRS", 200, responseJson)
        stubGet(s"/paye-registration/$txId/registration-id", 200, Json.toJson(regId).toString)
        stubDelete(s"/save4later/paye-registration-frontend/$regId", 204, "")
        stubDelete(s"/paye-registration/$regId/delete-rejected-incorp", 200, "")

        val res = await(sessionProfile.withCurrentProfile(testFunc))
        res.header.status mustBe SEE_OTHER

        val crDeleteS4L = findAll(deleteRequestedFor(urlMatching(s"/save4later/paye-registration-frontend/$regId")))
        val captor = crDeleteS4L.get(0)
        captor.getUrl.contains(regId) mustBe true

        val crDeletePAYEReg = findAll(deleteRequestedFor(urlMatching(s"/paye-registration/$regId/delete-rejected-incorp")))
        val captor2 = crDeletePAYEReg.get(0)
        captor2.getUrl.contains(regId) mustBe true
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