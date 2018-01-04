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

package service

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{Failed, KeystoreConnector, PAYERegistrationConnector, Success}
import enums.CacheKeys
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class SubmissionServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnector]
  lazy val keystoreConnector         = app.injector.instanceOf[KeystoreConnector]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng=="
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  val sId = UUID.randomUUID().toString
  implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

  def currentProfile(regId: String) = CurrentProfile(
    registrationID = regId,
    companyTaxRegistration = CompanyRegistrationProfile(
      status = "acknowledged",
      transactionId = "40-123456"
    ),
    language = "ENG",
    payeRegistrationSubmitted = false
  )

  "submitRegistration" should {
    "NOT send the submission if the regId is in whitelist" in {
      val regIdWhitelisted = "regWhitelist123"

      val submissionService = new SubmissionService(payeRegistrationConnector, keystoreConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regIdWhitelisted))

      an[Exception] shouldBe thrownBy(await(getResponse))
    }

    "send the submission and update keystore if the regId is not in whitelist" in {
      val regId = "12345"

      stubPut(s"/paye-registration/$regId/submit-registration", 200, "")
      stubKeystoreCache(sId, CacheKeys.CurrentProfile.toString)

      val submissionService = new SubmissionService(payeRegistrationConnector, keystoreConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regId))

      await(getResponse) shouldBe Success

      verify(putRequestedFor(urlEqualTo(s"/keystore/paye-registration-frontend/$sId/data/${CacheKeys.CurrentProfile.toString}"))
        .withRequestBody(
          equalToJson(Json.parse(
            s"""{
               |  "registrationID":"12345",
               |  "companyTaxRegistration":{
               |    "status":"acknowledged",
               |    "transactionId":"40-123456"
               |  },"language":"ENG",
               |  "payeRegistrationSubmitted":true
               |}
             """.stripMargin).toString)
        )
      )
    }

    "send the submission and leave keystore unchanged if the DES submission fails" in {
      val regId = "12345"

      stubPut(s"/paye-registration/$regId/submit-registration", 400, "")
      stubKeystoreCache(sId, CacheKeys.CurrentProfile.toString)

      val submissionService = new SubmissionService(payeRegistrationConnector, keystoreConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regId))

      await(getResponse) shouldBe Failed

      verify(0, putRequestedFor(urlEqualTo(s"/keystore/paye-registration-frontend/$sId/data/${CacheKeys.CurrentProfile.toString}")))
    }
  }
}
