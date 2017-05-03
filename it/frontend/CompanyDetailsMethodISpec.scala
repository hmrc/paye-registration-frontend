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

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeApplication
import play.api.http.HeaderNames


class CompanyDetailsMethodISpec extends IntegrationSpecBase
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
    "defaultSeqDirector" -> "W3siZGlyZWN0b3IiOnsiZm9yZW5hbWUiOiJmYXVsdHkiLCJzdXJuYW1lIjoiZGVmYXVsdCJ9fV0="
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val companyName = "Foo Ltd"


  "GET Trading Name" should {

    "Return a populated page if PayeReg returns an company details response" in {
      val txId = "12345"
      val tradingName = "Foo Trading"
      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")

      val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
      val payeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "${tradingName}",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc},
           |"businessContactDetails": {}
           |}""".stripMargin
      stubGet(s"/paye-registration/${regId}/company-details", 200, payeDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Trading name"
      document.getElementById("pageHeading").text should include(companyName)
      document.getElementById("differentName-true").attr("checked") shouldBe "checked"
      document.getElementById("differentName-false").attr("checked") shouldBe ""
      document.getElementById("tradingName").`val` shouldBe tradingName
    }

    "Return an unpopulated page if PayeReg returns a NotFound response" in {
      val txId = "12345"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/${regId}/company-details", 404, "")
      val crDoc = s"""{"transaction-id": "${txId}"}"""
      stubGet(s"/incorporation-frontend-stubs/corporation-tax-registration/${regId}/confirmation-references", 200, crDoc)
      val roDoc = s"""{"premises":"p", "address_line_1":"1", "locality":"l"}"""
      stubGet(s"/incorporation-frontend-stubs/${txId}/ro-address", 200, roDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Trading name"
      document.getElementById("pageHeading").text should include(companyName)
      document.getElementById("differentName-true").attr("checked") shouldBe ""
      document.getElementById("differentName-false").attr("checked") shouldBe ""
      document.getElementById("tradingName").`val` shouldBe ""

      // TODO verify S4L put
    }

    "Return a populated page with a default Company Name if the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystoreMetadata(SessionId, regIdWhitelisted, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regIdWhitelisted}", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regIdWhitelisted}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Trading name"
      document.getElementById("pageHeading").text should include(defaultCompanyName)
    }
  }

  "POST Trading Name" should {

    "Accept information and send to PR" in {
      val txId = "12345"
      val tradingName = "Foo Trading"

      setupSimpleAuthMocks()

      val csrfToken = UUID.randomUUID().toString

      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
      val payeDoc =s"""{
           |"companyName": "${companyName}",
           |"tradingName": {"differentName":false},
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc},
           |"businessContactDetails": {}
           |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "${tradingName}",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc},
           |"businessContactDetails": {}
           |}""".stripMargin
      stubPatch(s"/paye-registration/${regId}/company-details", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "differentName"->Seq("true"),
          "tradingName"->Seq(tradingName)
        ))

      val response = await(fResponse)

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/registered-office-address")

      val crPuts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/${regId}/company-details")))
      val captor = crPuts.get(0)
      val json = Json.parse(captor.getBodyAsString)
      (json \ "tradingName").as[String] shouldBe tradingName
    }
  }

  "GET RO Address page" should {
    "show the page with a default RO Address if the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubKeystoreMetadata(SessionId, regIdWhitelisted, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regIdWhitelisted}", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regIdWhitelisted}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/registered-office-address").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Confirm the company's registered office address"
      document.getElementById("companyName").text shouldBe defaultCompanyName
      document.getElementById("ro-address-address-line-1").text shouldBe "14 Test Default Street"
    }
  }

  "GET PPOB page" should {

    "show the page with the RO Address when there is no PPOB Address" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/principal-place-of-business").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Tell us where you'll carry out most of your business activities"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() shouldBe 0

    }

    "show the page with the PPOB Address when PPOB Address is the same as RO Address" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"ppobAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/principal-place-of-business").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Tell us where you'll carry out most of your business activities"
      document.getElementById("ppob-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ro-address-line-1").size() shouldBe 0

    }

    "show the page with both PPOB and RO Addresses when there are two different addresses" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val ppobDoc = s"""{"line1":"22", "line2":"23", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"ppobAddress": ${ppobDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/principal-place-of-business").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Tell us where you'll carry out most of your business activities"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementById("ppob-address-line-1").text shouldBe "22"

    }


  }

  "POST PPOB Address" should {
    "save to microservice with full company details data" in {
      setupSimpleAuthMocks()

      val csrfToken = UUID.randomUUID().toString

      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "tName",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc},
           |"businessContactDetails": {}
           |}""".stripMargin
      stubPatch(s"/paye-registration/${regId}/company-details", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      val fResponse = buildClient("/principal-place-of-business").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "chosenAddress"->Seq("roAddress")
        ))

      val response = await(fResponse)

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/business-contact-details")

      val crPuts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/${regId}/company-details")))
      val captor = crPuts.get(0)
      val json = Json.parse(captor.getBodyAsString)
      (json \ "ppobAddress").as[JsObject].toString() shouldBe roDoc
    }


    "save to save for later with incomplete company details data" in {
      setupSimpleAuthMocks()

      val csrfToken = UUID.randomUUID().toString

      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc}
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "tName",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc}
           |}""".stripMargin
      stubS4LPut(regId, "CompanyDetails", updatedPayeDoc)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      val fResponse = buildClient("/principal-place-of-business").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "chosenAddress"->Seq("roAddress")
        ))

      val response = await(fResponse)

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/business-contact-details")

    }
  }
}
