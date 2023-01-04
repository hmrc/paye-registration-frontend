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

import config.AppConfig
import connectors._
import enums.{CacheKeys, IncorporationStatus}
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.external.{CompanyRegistrationProfile, CurrentProfile}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.util.UUID
import scala.concurrent.ExecutionContext

class SubmissionServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val config = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
    "mongodb.uri" -> s"$mongoUri"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnector]
  lazy val keystoreConnector = app.injector.instanceOf[KeystoreConnector]
  lazy val incorpInfoConnector = app.injector.instanceOf[IncorporationInformationConnector]
  implicit lazy val appConfig = app.injector.instanceOf[AppConfig]
  lazy implicit val ec = app.injector.instanceOf[ExecutionContext]

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
    "NOT send the submission if the regId is in allow-list" in {
      val regIdAllowlisted = "regAllowlist123"

      val submissionService = new SubmissionServiceImpl(payeRegistrationConnector, keystoreConnector, incorpInfoConnector)

      def getResponse = submissionService.submitRegistration(currentProfile(regIdAllowlisted))

      an[Exception] mustBe thrownBy(await(getResponse))
    }

    "send the submission and update keystore if the regId is not in allow-list" in {
      val regId = "12345"
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

    "send the submission, update keystore and cancel subscription if the regId is not in allow-list" in {
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
