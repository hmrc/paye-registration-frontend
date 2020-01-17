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

package services

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors._
import enums.{CacheKeys, IncorporationStatus}
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import services.SubmissionServiceImpl
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class SubmissionServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnector]
  lazy val keystoreConnector         = app.injector.instanceOf[KeystoreConnector]
  lazy val incorpInfoConnector       = app.injector.instanceOf[IncorporationInformationConnector]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "mongodb.uri" -> s"$mongoUri"
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
    payeRegistrationSubmitted = false,
    incorpStatus = None
  )

  "submitRegistration" should {
    "NOT send the submission if the regId is in whitelist" in {
      val regIdWhitelisted = "regWhitelist123"

      val submissionService = new SubmissionServiceImpl(payeRegistrationConnector, keystoreConnector, incorpInfoConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regIdWhitelisted))

      an[Exception] mustBe thrownBy(await(getResponse))
    }

    "send the submission and update keystore if the regId is not in whitelist" in {
      val regId         = "12345"
      val transactionId = "40-123456"

      stubPut(s"/paye-registration/$regId/submit-registration", 200, "")
      stubDelete(s"/incorporation-information/subscribe/$transactionId/regime/paye-fe/subscriber/SCRS", 200, "")

      val submissionService = new SubmissionServiceImpl(payeRegistrationConnector, keystoreConnector, incorpInfoConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regId))

      await(getResponse) mustBe Success

      verifySessionCacheData[CurrentProfile](sId, CacheKeys.CurrentProfile.toString, Some(
        CurrentProfile(
          registrationID = "12345",
          companyTaxRegistration = CompanyRegistrationProfile(
            status = "acknowledged",
            transactionId = "40-123456"
          ),
          language = "ENG",
          payeRegistrationSubmitted = true,
          incorpStatus = None
        )))
    }

    "send the submission, update keystore and cancel subscription if the regId is not in whitelist" in {
      val regId = "12345"
      val transactionId = "40-123456"

      stubPut(s"/paye-registration/$regId/submit-registration", 204, "")
      stubDelete(s"/incorporation-information/subscribe/$transactionId/regime/paye-fe/subscriber/SCRS", 404, "")

      val submissionService = new SubmissionServiceImpl(payeRegistrationConnector, keystoreConnector, incorpInfoConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regId))

      await(getResponse) mustBe Cancelled

      verifySessionCacheData[CurrentProfile](sId, CacheKeys.CurrentProfile.toString, Some(
        CurrentProfile(
          registrationID = "12345",
          companyTaxRegistration = CompanyRegistrationProfile(
            status = "acknowledged",
            transactionId = "40-123456"
          ),
          language = "ENG",
          payeRegistrationSubmitted = false,
          incorpStatus = Some(IncorporationStatus.rejected)
        )))
    }

    "send the submission and leave keystore unchanged if the DES submission fails" in {
      val regId = "12345"

      stubPut(s"/paye-registration/$regId/submit-registration", 400, "")

      val submissionService = new SubmissionServiceImpl(payeRegistrationConnector, keystoreConnector, incorpInfoConnector)
      def getResponse = submissionService.submitRegistration(currentProfile(regId))

      await(getResponse) mustBe Failed

      verifySessionCacheData[CurrentProfile](sId, CacheKeys.CurrentProfile.toString, None)
    }
  }
}
