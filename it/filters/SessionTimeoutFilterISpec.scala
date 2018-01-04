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

package filters

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.test.FakeApplication

class SessionTimeoutFilterISpec extends IntegrationSpecBase
                                with LoginStub
                                with CachingStub
                                with BeforeAndAfterEach
                                with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.host" -> s"$mockHost",
    "microservice.services.cachable.session-cache.port" -> s"$mockPort",
    "microservice.services.cachable.session-cache.domain" -> "keystore",
    "microservice.services.cachable.short-lived-cache.host" -> s"$mockHost",
    "microservice.services.cachable.short-lived-cache.port" -> s"$mockPort",
    "microservice.services.cachable.short-lived-cache.domain" -> "save4later",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.paye-registration.host" -> s"$mockHost",
    "microservice.services.paye-registration.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "session.timeoutSeconds" -> "10"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val txId = "12345"
  val companyName = "Foo Ltd"

  "GET trading name" should {
    "redirect to the sign in page" when {
      "the user has signed in but the session has timed out" in {
        val tradingName = "Foo Trading"
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubPayeRegDocumentStatus(regId)
        stubKeystoreMetadata(SessionId, regId)

        val companyProfileDoc =
          """
            |{
            |  "company_name":"$companyName",
            |  "registered_office_address":{
            |    "premises":"1",
            |    "address_line_1":"test street",
            |    "locality":"Testford",
            |    "country":"UK",
            |    "postal_code":"TE2 2ST"
            |  }
            |}
          """.stripMargin
        stubGet(s"/incorporation-information/$txId/company-profile", 200, companyProfileDoc)
        stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

        val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
        val payeDoc =
          s"""{
             |"companyName": "$companyName",
             |"tradingName": "$tradingName",
             |"roAddress": $roDoc,
             |"ppobAddress": $roDoc,
             |"businessContactDetails": {}
             |}""".stripMargin
        stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
        val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

        val fResponse = buildClient("/trading-name").
          withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
          get()

        val response = await(fResponse)

        response.status shouldBe 200

        val fResponse2 = buildClient("/trading-name").
          withHeaders(HeaderNames.COOKIE -> getSessionCookie(timeStampRollback = 70000)).
          get()

        val responseWithRollback = await(fResponse2)
        responseWithRollback.status shouldBe 303
        responseWithRollback.header(HeaderNames.LOCATION).get.contains("sign-in?accountType=organisation") shouldBe true
      }
    }
  }
}
