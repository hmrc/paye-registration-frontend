/*
 * Copyright 2017 HM Revenue & Customs
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

package service

import connectors.{BusinessRegistrationConnector, CompanyRegistrationConnector, KeystoreConnector}
import itutil.{IntegrationSpecBase, WiremockHelper}
import models.external.{BusinessProfile, CompanyProfile, CurrentProfile}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.{Application, Play}
import services.CurrentProfileService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId

class CurrentProfileServiceISpec extends IntegrationSpecBase {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val businessRegistrationConnector = Play.current.injector.instanceOf[BusinessRegistrationConnector]
  lazy val keystoreConnector = Play.current.injector.instanceOf[KeystoreConnector]
  lazy val companyRegistrationConnector = Play.current.injector.instanceOf[CompanyRegistrationConnector]

  val additionalConfiguration = Map(
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.coho-api.host" -> s"$mockHost",
    "microservice.services.coho-api.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ=="
  )

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
        None,
        "ENG"
      )

      val expectedCurrentProfile = CurrentProfile(regIdWhitelisted,
                                                  None,
                                                  CompanyProfile("held", s"fakeTxId-$regIdWhitelisted"),
                                                  "ENG")

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfileWithRegIdWhitelisted).toString)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/keystore/paye-registration-frontend/${sessionId}/data/CurrentProfile", 200, dummyS4LResponse)

      val currentProfileService = new CurrentProfileService(businessRegistrationConnector, keystoreConnector, companyRegistrationConnector)
      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) shouldBe expectedCurrentProfile
    }

    "get a Current Profile" in {
      val regId = "12345"
      val businessProfile = BusinessProfile(
        regId,
        None,
        "ENG"
      )

      val companyRegistrationResp =
        s"""{
          |   "status": "held",
          |   "confirmationReferences": {
          |     "transaction-id": "000-434-${regId}"
          |   }
          |}""".stripMargin

      val expectedCurrentProfile = CurrentProfile(regId,
        None,
        CompanyProfile("held", s"000-434-$regId"),
        "ENG")

      stubGet(s"/business-registration/business-tax-registration", 200, Json.toJson(businessProfile).toString)
      stubGet(s"/incorporation-frontend-stubs/$regId", 200, companyRegistrationResp)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/keystore/paye-registration-frontend/${sessionId}/data/CurrentProfile", 200, dummyS4LResponse)

      val currentProfileService = new CurrentProfileService(businessRegistrationConnector, keystoreConnector, companyRegistrationConnector)
      def getResponse = currentProfileService.fetchAndStoreCurrentProfile

      await(getResponse) shouldBe expectedCurrentProfile
    }
  }
}
