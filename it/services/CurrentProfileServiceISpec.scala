/*
 * Copyright 2018 HM Revenue & Customs
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
import connectors.{BusinessRegistrationConnector, CompanyRegistrationConnector, KeystoreConnector, PAYERegistrationConnector}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external.{BusinessProfile, CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.CurrentProfileServiceImpl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class CurrentProfileServiceISpec extends IntegrationSpecBase {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val businessRegistrationConnector = app.injector.instanceOf[BusinessRegistrationConnector]
  lazy val keystoreConnector             = app.injector.instanceOf[KeystoreConnector]
  lazy val companyRegistrationConnector  = app.injector.instanceOf[CompanyRegistrationConnector]
  lazy val payeRegistrationConnector     = app.injector.instanceOf[PAYERegistrationConnector]

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
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ=="
  )

  def backendStatus(status: String) = {
    s"""{
     |   "status": "$status",
     |   "lastUpdate": "2017-05-01T12:00:00Z",
     |   "ackRef": "testAckRef",
     |   "empref": "testEmpRef"
     |}""".stripMargin
  }

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val sessionId = "session-123"
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sessionId)))

  "fetchAndStoreCurrentProfile" should {
    "get a default Current Profile when the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val businessProfileWithRegIdWhitelisted = BusinessProfile(
        regIdWhitelisted,
        "ENG"
      )

      val expectedCurrentProfile = CurrentProfile(regIdWhitelisted,
                                                  CompanyRegistrationProfile("held", s"fakeTxId-$regIdWhitelisted"),
                                                  "ENG",
                                                  payeRegistrationSubmitted = false,
                                                  incorpStatus = None)

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfileWithRegIdWhitelisted).toString)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/keystore/paye-registration-frontend/${sessionId}/data/CurrentProfile", 200, dummyS4LResponse)
      stubGet(s"/paye-registration/$regIdWhitelisted/status", 200, backendStatus("draft"))

      val currentProfileService = new CurrentProfileServiceImpl(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector)
      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) mustBe expectedCurrentProfile
    }

    "get a Current Profile while stubbed" in {
      val regId = "12345"
      val businessProfile = BusinessProfile(
        regId,
        "ENG"
      )

      val companyRegistrationResp =
        s"""{
          |   "status": "held",
          |   "confirmationReferences": {
          |     "transaction-id": "000-434-${regId}"
          |   }
          |}""".stripMargin

      val expectedCurrentProfile = CurrentProfile(
        regId,
        CompanyRegistrationProfile("held", s"000-434-$regId"),
        "ENG",
        payeRegistrationSubmitted = false,
        incorpStatus = None)

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
      stubGet(s"/incorporation-frontend-stubs/$regId/corporation-tax-registration", 200, companyRegistrationResp)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/keystore/paye-registration-frontend/${sessionId}/data/CurrentProfile", 200, dummyS4LResponse)
      stubGet(s"/paye-registration/$regId/status", 404, "")

      val currentProfileService = new CurrentProfileServiceImpl(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector)
      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) mustBe expectedCurrentProfile
    }

    "get a Current Profile" in {
      val regId = "12345"
      val businessProfile = BusinessProfile(
        regId,
        "ENG"
      )

      stubFor(post(urlMatching("/write/audit"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody("""{"x":2}""")
        )
      )

      await(buildClient("/test-only/feature-flag/companyRegistration/true").get())

      val companyRegistrationResp =
        s"""{
          |   "status": "held",
          |   "confirmationReferences": {
          |     "transaction-id": "000-434-${regId}"
          |   }
          |}""".stripMargin

      val expectedCurrentProfile = CurrentProfile(regId,
        CompanyRegistrationProfile("held", s"000-434-$regId"),
        "ENG",
        payeRegistrationSubmitted = true,
        incorpStatus = None)

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
      stubGet(s"/company-registration/corporation-tax-registration/$regId/corporation-tax-registration", 200, companyRegistrationResp)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/keystore/paye-registration-frontend/${sessionId}/data/CurrentProfile", 200, dummyS4LResponse)
      stubGet(s"/paye-registration/$regId/status",200,backendStatus("submitted"))

      val currentProfileService = new CurrentProfileServiceImpl(businessRegistrationConnector, payeRegistrationConnector, keystoreConnector, companyRegistrationConnector)
      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) mustBe expectedCurrentProfile
    }
  }
}