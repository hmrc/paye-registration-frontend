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
import connectors.{BusinessRegistrationConnector, KeystoreConnector, PAYERegistrationConnector}
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
  lazy val busRegConnector = Play.current.injector.instanceOf[BusinessRegistrationConnector]

  val additionalConfiguration = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.business-registration.port" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
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

      stubFor(get(urlMatching(s"/business-registration/business-tax-registration"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody(
              """
                |{
                | "registrationID" : "1234",
                | "completionCapacity" : "Director",
                | "language" : "EN"
                |}
                |"""".stripMargin)
        )
      )

      val tstCap = new CompletionCapacityService(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res shouldBe Some(CompletionCapacity(UserCapacity.director, ""))
    }
  }

  "Return no Completion Capacity from BR (returns 404)" in {

    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

    stubFor(get(urlMatching(s"/business-registration/business-tax-registration"))
      .willReturn(
        aResponse().
          withStatus(404)
      )
    )

    val tstCap = new CompletionCapacityService(payeRegistrationConnector, busRegConnector)
    val res = await(tstCap.getCompletionCapacity(regID))

    res shouldBe None

  }

  "Return None if there is no completion capacity in the BR document" in {

    implicit val hc = HeaderCarrier(sessionId = Some(SessionId(sId)))

    stubFor(get(urlMatching(s"/business-registration/business-tax-registration"))
      .willReturn(
        aResponse().
          withStatus(200).
          withBody(
            """
              |{
              | "registrationID" : "1234",
              | "language" : "EN"
              |}
              |"""".stripMargin)
      )
    )

    val tstCap = new CompletionCapacityService(payeRegistrationConnector, busRegConnector)
    val res = await(tstCap.getCompletionCapacity(regID))

    res shouldBe None

  }

}
