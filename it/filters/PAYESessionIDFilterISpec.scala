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

package filters

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner

class PAYESessionIDFilterISpec extends IntegrationSpecBase
  with LoginStub
  with CachingStub
  with BeforeAndAfterEach
  with WiremockHelper {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl = s"http://$mockHost:$mockPort"

  lazy val config = Map(
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
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "mongodb.uri" -> s"$mongoUri"
  )

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  lazy val defaultCookieSigner: DefaultCookieSigner = app.injector.instanceOf[DefaultCookieSigner]

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"

  "Loading a page" should {
    "redirect to post-sign-in" when {
      "sessionId is invalid" in {
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubSessionCacheMetadata(SessionId, regId)

        stubGet(s"/paye-registration/$regId/company-details", 404, "")
        stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
        stubGet(s"/business-registration/$regId/contact-details", 403, "")
        stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
        val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

        val response = await(buildClient("/who-should-we-contact")
          .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(sessionID = invalidSessionId))
          .get())

        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/post-sign-in")
      }
      "user is not authorised" in {
        setupUnauthorised()

        val response = await(buildClient("/who-should-we-contact")
          .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie(sessionID = invalidSessionId))
          .get())

        response.status mustBe 303
        response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/post-sign-in")
      }
    }
    "continue as normal" when {
      "sessionId is Valid" in {
        setupSimpleAuthMocks()
        stubSuccessfulLogin()
        stubSessionCacheMetadata(SessionId, regId)

        stubGet(s"/paye-registration/$regId/company-details", 404, "")
        stubGet(s"/paye-registration/$regId/contact-correspond-paye", 404, "")
        stubGet(s"/business-registration/$regId/contact-details", 403, "")
        stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
        val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)
        stubPut(s"/save4later/paye-registration-frontend/$regId/data/PAYEContact", 200, dummyS4LResponse)

        val response = await(buildClient("/who-should-we-contact")
          .withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie())
          .get())

        response.status mustBe 200

        val document = Jsoup.parse(response.body)
        document.title() mustBe "Who should we contact about the company's PAYE?"
        document.getElementById("name").data() mustBe ""
        document.getElementById("digitalContact.contactEmail").attr("value") mustBe ""
        document.getElementById("digitalContact.mobileNumber").attr("value") mustBe ""
        document.getElementById("digitalContact.phoneNumber").attr("value") mustBe ""
      }
    }
  }
}
