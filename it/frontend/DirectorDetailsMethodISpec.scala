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

package frontend

import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.test.FakeApplication

/**
  * Created by henrilay on 03/05/2017.
  */
class DirectorDetailsMethodISpec extends IntegrationSpecBase
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
    "microservice.services.coho-api.host" -> s"$mockHost",
    "microservice.services.coho-api.port" -> s"$mockPort",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJwcmVtaXNlcyI6IjE0IiwiYWRkcmVzc19saW5lXzEiOiJUZXN0IERlZmF1bHQgU3RyZWV0IiwiYWRkcmVzc19saW5lXzIiOiJUZXN0bGV5IiwibG9jYWxpdHkiOiJUZXN0Zm9yZCIsImNvdW50cnkiOiJVSyIsInBvc3RhbF9jb2RlIjoiVEUxIDFTVCJ9",
    "defaultSeqDirector" -> "W3siZGlyZWN0b3IiOnsiZm9yZW5hbWUiOiJmYXVsdHkiLCJzdXJuYW1lIjoiZGVmYXVsdCJ9fSx7ImRpcmVjdG9yIjp7ImZvcmVuYW1lIjoiVGVzdCIsInN1cm5hbWUiOiJSZWdJZFdoaXRlbGlzdCIsInRpdGxlIjoiTXJzIn19XQ=="
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val companyName = "Foo Ltd"

  "GET Director Details" should {
    "show the page with a default list of Directors if the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regIdWhitelisted)

      stubKeystoreMetadata(SessionId, regIdWhitelisted, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regIdWhitelisted}", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regIdWhitelisted}/data/DirectorDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/director-national-insurance-number").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Enter the National Insurance number of at least one company director"
      document.getElementsByClass("form-field").size shouldBe 2

      val list = document.getElementsByClass("form-label")
      def get(n: Int) = list.get(n).text

      get(0) shouldBe s"faulty default's National Insurance number For example, QQ 12 34 56 C"
      get(1) shouldBe s"Test RegIdWhitelist's National Insurance number"
    }
  }
}
