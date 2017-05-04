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

import connectors.{PAYERegistrationConnector, Success}
import itutil.{IntegrationSpecBase, WiremockHelper}
import play.api.{Application, Play}
import play.api.inject.guice.GuiceApplicationBuilder
import services.SubmissionService
import uk.gov.hmrc.play.http.HeaderCarrier

class SubmissionServiceISpec extends IntegrationSpecBase {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val payeRegistrationConnector = Play.current.injector.instanceOf[PAYERegistrationConnector]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng=="
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  implicit val hc = HeaderCarrier()

  "submitRegistration" should {
    "NOT send the submission if the regId is in whitelist" in {
      val regIdWhitelisted = "regWhitelist123"

      val submissionService = new SubmissionService(payeRegistrationConnector)
      def getResponse = submissionService.submitRegistration(regIdWhitelisted)

      an[Exception] shouldBe thrownBy(await(getResponse))
    }

    "send the submission if the regId is not in whitelist" in {
      val regId = "12345"

      stubPut(s"/paye-registration/$regId/submit-registration", 200, "")

      val submissionService = new SubmissionService(payeRegistrationConnector)
      def getResponse = submissionService.submitRegistration(regId)

      await(getResponse) shouldBe Success
    }
  }
}
