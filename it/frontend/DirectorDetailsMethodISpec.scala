/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.Application
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.crypto.DefaultCookieSigner

class DirectorDetailsMethodISpec extends IntegrationSpecBase
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
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "regIdAllowlist" -> "cmVnQWxsb3dsaXN0MTIzLHJlZ0FsbG93bGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJwcmVtaXNlcyI6IjE0IiwiYWRkcmVzc19saW5lXzEiOiJUZXN0IERlZmF1bHQgU3RyZWV0IiwiYWRkcmVzc19saW5lXzIiOiJUZXN0bGV5IiwibG9jYWxpdHkiOiJUZXN0Zm9yZCIsImNvdW50cnkiOiJVSyIsInBvc3RhbF9jb2RlIjoiVEUxIDFTVCJ9",
    "defaultSeqDirector" -> "W3siZGlyZWN0b3IiOnsiZm9yZW5hbWUiOiJmYXVsdHkiLCJzdXJuYW1lIjoiZGVmYXVsdCJ9fSx7ImRpcmVjdG9yIjp7ImZvcmVuYW1lIjoiVGVzdCIsInN1cm5hbWUiOiJSZWdJZFdoaXRlbGlzdCIsInRpdGxlIjoiTXJzIn19XQ==",
    "defaultOfficerList" -> "WwogICAgewogICAgICAibmFtZSIgOiAidGVzdCIsCiAgICAgICJuYW1lX2VsZW1lbnRzIiA6IHsKICAgICAgICAiZm9yZW5hbWUiIDogInRlLHN0MSIsCiAgICAgICAgIm90aGVyX2ZvcmVuYW1lcyIgOiAidGUsc3QxMSIsCiAgICAgICAgInN1cm5hbWUiIDogInRlc3QsYSIsCiAgICAgICAgInRpdGxlIiA6ICJNLC5yIgogICAgICB9LAogICAgICAib2ZmaWNlcl9yb2xlIiA6ICJkaXJlY3RvciIKICAgIH0sIHsKICAgICAgIm5hbWUiIDogInRlc3QiLAogICAgICAibmFtZV9lbGVtZW50cyIgOiB7CiAgICAgICAgImZvcmVuYW1lIiA6ICJ0ZXN0MiIsCiAgICAgICAgIm90aGVyX2ZvcmVuYW1lcyIgOiAidGVzdDIyIiwKICAgICAgICAic3VybmFtZSIgOiAidGVzdGIiLAogICAgICAgICJ0aXRsZSIgOiAiTXIiCiAgICAgIH0sCiAgICAgICJvZmZpY2VyX3JvbGUiIDogImRpcmVjdG9yIgogICAgfSwgewogICAgICAibmFtZSIgOiAidGVzdCIsCiAgICAgICJuYW1lX2VsZW1lbnRzIiA6IHsKICAgICAgICAiZm9yZW5hbWUiIDogInRlc3QzIiwKICAgICAgICAib3RoZXJfZm9yZW5hbWVzIiA6ICJ0ZXN0MzMiLAogICAgICAgICJzdXJuYW1lIiA6ICJ0ZXN0YyIsCiAgICAgICAgInRpdGxlIiA6ICJUZXN0IFRpdGxlIFRoYXQgSXMgTW9yZSBUaGFuIFR3ZW50eSBDaGFycyIKICAgICAgfSwKICAgICAgIm9mZmljZXJfcm9sZSIgOiAiZGlyZWN0b3IiCiAgICB9CiAgXQ==",
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
  val companyName = "Foo Ltd"

  "GET Director Details" should {
    "show the page with a default list of Directors if the regId is part of the allow-list" in {
      val regIdAllowlisted = "regAllowlist123"

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regIdAllowlisted)
      stubSessionCacheMetadata(SessionId, regIdAllowlisted)

      stubGet(s"/save4later/paye-registration-frontend/$regIdAllowlisted", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regIdAllowlisted/data/DirectorDetails", 200, dummyS4LResponse)
      val fResponse = buildClient("/director-national-insurance-number").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() must include("What is the National Insurance number of at least one company director?")
      document.getElementsByClass("form-field").size mustBe 3

      val list = document.getElementsByClass("form-label")

      def get(n: Int) = list.get(n).text


      get(0) mustBe s"Mr test1 test11 testa's National Insurance number For example, QQ 12 34 56 C"
      get(1) mustBe s"Mr test2 test22 testb's National Insurance number"
      get(2) mustBe s"test3 test33 testc's National Insurance number"
    }

    "not show any officers who aren't directors or directors who are retired" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubGet(s"/paye-registration/$regId/directors", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/DirectorDetails", 200, dummyS4LResponse)

      val tstOfficerListJson =
        """
          |{
          |  "officers": [
          |    {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "test,1",
          |        "other_forenames" : ",test11",
          |        "surname" : "te, sta",
          |        "title" : "Mr,"
          |      },
          |      "officer_role" : "director",
          |      "resigned_on" : "2017-01-01"
          |    }, {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "test2",
          |        "other_forenames" : "test22",
          |        "surname" : "testb",
          |        "title" : "Mr"
          |      },
          |      "officer_role" : "corporate-director"
          |    }
          |  ]
          |}""".stripMargin
      stubGet(s"/incorporation-information/12345/officer-list", 200, tstOfficerListJson)

      val fResponse = buildClient("/director-national-insurance-number").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.getElementsByClass("form-field").size mustBe 1

      val list = document.getElementsByClass("form-label")

      def get(n: Int) = list.get(n).text

      get(0).contains("Error fetching name") mustBe true
    }

    "should show a list of directors" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubGet(s"/paye-registration/$regId/directors", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/DirectorDetails", 200, dummyS4LResponse)

      val tstOfficerListJson =
        """
          |{
          |  "officers": [
          |    {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "test1",
          |        "other_forenames" : "test11",
          |        "surname" : "testa",
          |        "title" : "Mr"
          |      },
          |      "officer_role" : "director"
          |    },
          |    {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "test2",
          |        "other_forenames" : "test22",
          |        "surname" : "testb",
          |        "title" : "Test Title That Is Over Twenty Chars"
          |      },
          |      "officer_role" : "director"
          |    },
          |    {
          |      "name" : "abc",
          |      "name_elements" : {
          |        "forename" : "a",
          |        "other_forenames" : "b",
          |        "surname" : "c"
          |      },
          |      "officer_role" : "director"
          |    }, {
          |      "name" : "test",
          |      "name_elements" : {
          |        "forename" : "test3",
          |        "other_forenames" : "test33",
          |        "surname" : "testc",
          |        "title" : "Mr"
          |      },
          |      "officer_role" : "corporate-nominee-director"
          |    }
          |  ]
          |}""".stripMargin
      stubGet(s"/incorporation-information/12345/officer-list", 200, tstOfficerListJson)

      val fResponse = buildClient("/director-national-insurance-number").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.getElementsByClass("form-field").size mustBe 3

      val list = document.getElementsByClass("form-label")

      def get(n: Int) = list.get(n).text

      get(0) mustBe s"Mr test1 test11 testa's National Insurance number For example, QQ 12 34 56 C"
      get(1) mustBe s"test2 test22 testb's National Insurance number"
      get(2) mustBe s"a b c's National Insurance number"
    }

    "should throw error when no valid directors are returned" in {
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")

      stubGet(s"/paye-registration/$regId/directors", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/DirectorDetails", 200, dummyS4LResponse)

      stubGet(s"/incorporation-information/12345/officer-list", 404, "")

      val fResponse = buildClient("/director-national-insurance-number").
        withHttpHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 500
    }
  }
}
