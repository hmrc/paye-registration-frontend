/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import config.AppConfig
import connectors._
import enums.IncorporationStatus
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.external.{BusinessProfile, CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext

class CurrentProfileServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val config = Map(
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
    "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "mongodb.uri" -> s"$mongoUri"
  )

  def backendStatus(status: String) = {
    s"""{
       |   "status": "$status",
       |   "lastUpdate": "2017-05-01T12:00:00Z",
       |   "ackRef": "testAckRef",
       |   "empref": "testEmpRef"
       |}""".stripMargin
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val businessRegistrationConnector = app.injector.instanceOf[BusinessRegistrationConnector]
  lazy val keystoreConnector = app.injector.instanceOf[KeystoreConnector]
  lazy val companyRegistrationConnector = app.injector.instanceOf[CompanyRegistrationConnector]
  lazy val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnector]
  lazy val incorpInfoConnector = app.injector.instanceOf[IncorporationInformationConnector]
  implicit lazy val appConfig = app.injector.instanceOf[AppConfig]
  override lazy implicit val ec = app.injector.instanceOf[ExecutionContext]

  val sessionId = "session-123"
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  "fetchAndStoreCurrentProfile" should {
    "get a default Current Profile when the regId is part of the allow-list" in {
      val regIdAllowlisted = "regAllowlist123"
      val businessProfileWithRegIdAllowlisted = BusinessProfile(
        regIdAllowlisted,
        "ENG"
      )

      val expectedCurrentProfile = CurrentProfile(regIdAllowlisted,
        CompanyRegistrationProfile("held", s"fakeTxId-$regIdAllowlisted"),
        "ENG",
        payeRegistrationSubmitted = false,
        incorpStatus = None)

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfileWithRegIdAllowlisted).toString)
      stubGet(s"/paye-registration/$regIdAllowlisted/status", 200, backendStatus("draft"))
      stubPost(s"/incorporation-information/subscribe/fakeTxId-$regIdAllowlisted/regime/paye-fe/subscriber/SCRS", 202, "")

      val currentProfileService = new CurrentProfileService(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector, incorpInfoConnector)

      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) mustBe expectedCurrentProfile
    }

    "get a Current Profile while stubbed" in {
      val regId = "12345"
      val txId = s"000-434-$regId"
      val businessProfile = BusinessProfile(
        regId,
        "ENG"
      )

      val companyRegistrationResp =
        s"""{
           |   "status": "held",
           |   "confirmationReferences": {
           |     "transaction-id": "$txId"
           |   }
           |}""".stripMargin

      val expectedCurrentProfile = CurrentProfile(
        regId,
        CompanyRegistrationProfile("held", txId),
        "ENG",
        payeRegistrationSubmitted = false,
        incorpStatus = None)

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
      stubGet(s"/incorporation-frontend-stubs/$regId/corporation-tax-registration", 200, companyRegistrationResp)
      stubGet(s"/paye-registration/$regId/status", 404, "")
      stubPost(s"/incorporation-information/subscribe/$txId/regime/paye-fe/subscriber/SCRS", 202, "")

      val currentProfileService = new CurrentProfileService(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector, incorpInfoConnector)

      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) mustBe expectedCurrentProfile

      verifySessionCacheData(sessionId, "CurrentProfile", Some(expectedCurrentProfile))
    }

    "get a Current Profile" when {
      val regId = "12345"
      val txId = s"000-434-$regId"
      val businessProfile = BusinessProfile(
        regId,
        "ENG"
      )
      val companyRegistrationResp =
        s"""{
           |   "status": "held",
           |   "confirmationReferences": {
           |     "transaction-id": "$txId"
           |   }
           |}""".stripMargin

      "II subscription returns 202" in {
        stubFor(post(urlMatching("/write/audit"))
          .willReturn(
            aResponse().
              withStatus(200).
              withBody("""{"x":2}""")
          )
        )

        await(buildClient("/test-only/feature-flag/companyRegistration/true").get())

        val expectedCurrentProfile = CurrentProfile(regId,
          CompanyRegistrationProfile("held", txId),
          "ENG",
          payeRegistrationSubmitted = true,
          incorpStatus = None)

        stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
        stubGet(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration", 200, companyRegistrationResp)
        stubGet(s"/paye-registration/$regId/status", 200, backendStatus("submitted"))
        stubPost(s"/incorporation-information/subscribe/$txId/regime/paye-fe/subscriber/SCRS", 202, "")

        val currentProfileService = new CurrentProfileService(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector, incorpInfoConnector)

        def getResponse = currentProfileService.fetchAndStoreCurrentProfile

        await(getResponse) mustBe expectedCurrentProfile

        verifySessionCacheData(sessionId, "CurrentProfile", Some(expectedCurrentProfile))
      }

      "II subscription returns 200 with json response" in {

        stubFor(post(urlMatching("/write/audit"))
          .willReturn(
            aResponse().
              withStatus(200).
              withBody("""{"x":2}""")
          )
        )

        await(buildClient("/test-only/feature-flag/companyRegistration/true").get())

        val iiSubscriptionRes = Json.parse(
          s"""
             |{
             |  "SCRSIncorpStatus": {
             |    "IncorpSubscriptionKey": {
             |      "subscriber": "SCRS",
             |      "discriminator": "paye-fe",
             |      "transactionId": "$txId"
             |    },
             |    "SCRSIncorpSubscription": {
             |      "callbackUrl": "/callBackUrl"
             |    },
             |    "IncorpStatusEvent": {
             |      "status": "rejected",
             |      "crn": "12345678",
             |      "incorporationDate": "2017-04-25T16:20:10.000+01:00",
             |      "description": "test",
             |      "timestamp": "2017-04-25T16:20:10.000+01:00"
             |    }
             |  }
             |}
       """.stripMargin).toString

        val expectedCurrentProfile = CurrentProfile(regId,
          CompanyRegistrationProfile("held", txId),
          "ENG",
          payeRegistrationSubmitted = true,
          incorpStatus = Some(IncorporationStatus.rejected))

        stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
        stubGet(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration", 200, companyRegistrationResp)
        stubGet(s"/paye-registration/$regId/status", 200, backendStatus("submitted"))
        stubPost(s"/incorporation-information/subscribe/$txId/regime/paye-fe/subscriber/SCRS", 200, iiSubscriptionRes)

        val currentProfileService = new CurrentProfileService(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector, incorpInfoConnector)

        def getResponse = currentProfileService.fetchAndStoreCurrentProfile

        await(getResponse) mustBe expectedCurrentProfile

        verifySessionCacheData(sessionId, "CurrentProfile", Some(expectedCurrentProfile))
      }
    }
  }
}