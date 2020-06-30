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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlMatching}
import connectors.{BusinessRegistrationConnector, PAYERegistrationConnector}
import enums.UserCapacity
import itutil.{CachingStub, IntegrationSpecBase, WiremockHelper}
import models.view.CompletionCapacity
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier

class CompletionCapacityServiceISpec extends IntegrationSpecBase with CachingStub {
  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  val config = Map(
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.business-registration.port" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val payeRegistrationConnector = app.injector.instanceOf[PAYERegistrationConnector]
  lazy val busRegConnector = app.injector.instanceOf[BusinessRegistrationConnector]

  implicit val hc = HeaderCarrier()

  val regID = "1234"
  val sId = UUID.randomUUID().toString

  "getCompletionCapacity" should {
    "return a completion capacity from PR if the PR document contains a CC" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody(""""director"""")
        )
      )

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe Some(CompletionCapacity(UserCapacity.director, ""))
    }

    "return a completion capacity from PR if the PR document contains a CC but is not director, secretary or agent" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody(""""friend"""")
        )
      )

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe Some(CompletionCapacity(UserCapacity.other, "friend"))
    }

    "return a completion capacity from BR if one is found in BR but NOT in PR" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(404).
            withBody(""""director"""")
        )
      )

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

      stubPatch(s"/paye-registration/$regID/capacity", 200, "\"director\"")

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe Some(CompletionCapacity(UserCapacity.director, ""))
    }

    "return a completion capacity from BR if one is found in BR but NOT in PR and it isn't director, secretary or agent" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(404)
        )
      )

      stubFor(get(urlMatching(s"/business-registration/business-tax-registration"))
        .willReturn(
          aResponse().
            withStatus(200).
            withBody(
              """
                |{
                | "registrationID" : "1234",
                | "completionCapacity" : "aunt",
                | "language" : "EN"
                |}
                |"""".stripMargin)
        )
      )

      stubPatch(s"/paye-registration/$regID/capacity", 200, "\"aunt\"")

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe Some(CompletionCapacity(UserCapacity.other, "aunt"))
    }

    "return none if no CC is found in either PR or BR (BR 404)" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(404)
        )
      )

      stubFor(get(urlMatching(s"/business-registration/business-tax-registration"))
        .willReturn(
          aResponse().
            withStatus(404)
        )
      )

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe None
    }

    "return none if no CC is found in either PR or BR (BR 200 no CC in document)" in {
      stubFor(get(urlMatching(s"/paye-registration/$regID/capacity"))
        .willReturn(
          aResponse().
            withStatus(404)
        )
      )

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

      val tstCap = new CompletionCapacityServiceImpl(payeRegistrationConnector, busRegConnector)
      val res = await(tstCap.getCompletionCapacity(regID))

      res mustBe None
    }
  }
}
