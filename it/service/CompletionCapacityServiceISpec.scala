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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import connectors.{KeystoreConnector, PAYERegistrationConnector}
import enums.UserCapacity
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import models.view.CompletionCapacity
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Play}
import services.CompletionCapacityService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.SessionId

class CompletionCapacityServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"


  lazy val payeRegistrationConnector = Play.current.injector.instanceOf[PAYERegistrationConnector]
  lazy val keystoreConnector = Play.current.injector.instanceOf[KeystoreConnector]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort"
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(additionalConfiguration)
    .build

  implicit val hc = HeaderCarrier()

  val regID = "1234"
  val sId = UUID.randomUUID().toString

  "getCompletionCapacity" should {
    "return a completion capacity if one is stored in the backend" in {

      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody(""""director"""")
        )
      )

      val tstCap = new CompletionCapacityService(payeRegistrationConnector, keystoreConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res shouldBe Some(CompletionCapacity(UserCapacity.director, ""))
    }
  }

  "Get Completion Capacity from Key Store if PR returns 404" in {

    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

    stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
      .willReturn(
        aResponse().
          withStatus(404)
      )
    )
    stubKeystoreMetadata(sId,regID,"tst company", "agent")

    val tstCap = new CompletionCapacityService(payeRegistrationConnector, keystoreConnector)
    val res = await(tstCap.getCompletionCapacity(regID))

    res shouldBe Some(CompletionCapacity(UserCapacity.agent, ""))

  }

  "Return None if there is no completion capacity in PR or Keystore" in {

    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

    stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
      .willReturn(
        aResponse().
          withStatus(404)
      )
    )

    val keystoreUrl = s"/keystore/paye-registration-frontend/$sId"
    stubFor(get(urlMatching(keystoreUrl))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            s"""{
               |"id": "$sId",
               |"data": {
               | "CurrentProfile": {
               |   "registrationID": "$regID",
               |   "companyTaxRegistration": {
               |      "status": "submitted",
               |      "transactionId": "12345"
               |   },
               |   "language": "ENG"
               |  }
               |}
               |}""".stripMargin
          )
      )
    )

    val tstCap = new CompletionCapacityService(payeRegistrationConnector, keystoreConnector)
    val res = await(tstCap.getCompletionCapacity(regID))

    res shouldBe None

  }

}
