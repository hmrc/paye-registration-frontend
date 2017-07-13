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
import enums.CacheKeys
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsObject, JsString, Json}
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
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJsaW5lMSI6IjE0IFRlc3QgRGVmYXVsdCBTdHJlZXQiLCJsaW5lMiI6IlRlc3RsZXkiLCJsaW5lMyI6IlRlc3Rmb3JkIiwibGluZTQiOiJUZXN0c2hpcmUiLCJwb3N0Q29kZSI6IlRFMSAzU1QifQ==",
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

      stubPayeRegDocumentStatus(regId)

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
      document.title() shouldBe "Will the company trade under another name?"
      document.getElementById("pageHeading").text shouldBe "Will the company trade under another name?"
      document.getElementById("differentName-true").attr("checked") shouldBe "checked"
      document.getElementById("differentName-false").attr("checked") shouldBe ""
      document.getElementById("tradingName").`val` shouldBe tradingName
    }

    "Return an unpopulated page if PayeReg returns a NotFound response" in {
      val txId = "12345"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/${regId}/company-details", 404, "")
      stubGet(s"/business-registration/${regId}/contact-details", 404, "")
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
      document.title() shouldBe "Will the company trade under another name?"
      document.getElementById("pageHeading").text shouldBe "Will the company trade under another name?"
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

      stubPayeRegDocumentStatus(regIdWhitelisted)

      stubKeystoreMetadata(SessionId, regIdWhitelisted, companyName)

      stubGet(s"/save4later/paye-registration-frontend/$regIdWhitelisted", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regIdWhitelisted/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Will the company trade under another name?"
      document.getElementById("pageHeading").text shouldBe "Will the company trade under another name?"
    }
  }

  "POST Trading Name" should {

    "Accept information and send to PR" in {
      val txId = "12345"
      val tradingName = "Foo Trading"

      setupSimpleAuthMocks()

      stubPayeRegDocumentStatus(regId)

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
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/confirm-registered-office-address")

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

      stubPayeRegDocumentStatus(regIdWhitelisted)

      stubGet(s"/save4later/paye-registration-frontend/${regIdWhitelisted}", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regIdWhitelisted}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/confirm-registered-office-address").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Confirm the company's registered office address"
      document.getElementById("lead-paragraph").text.contains(defaultCompanyName) shouldBe true
      document.getElementById("ro-address-address-line-1").text shouldBe "14 Test Default Street"
    }
  }

  "GET PPOB page" should {
    "show the page with the RO Address when there is no PPOB Address" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() shouldBe 0
    }

    "show the page with the PPOB Address when PPOB Address is the same as RO Address" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"ppobAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ppob-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ro-address-line-1").size() shouldBe 0

    }

    "show the page with both PPOB and RO Addresses when there are two different addresses" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

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

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementById("ppob-address-line-1").text shouldBe "22"

    }

    "show the page without prepop addresses if an error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 403, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() shouldBe 0
      an[Exception] shouldBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
    }

    "show the page without prepop addresses if a wrong address is returned from Business Registration" in {
      val addresses =
        s"""{
           |  "addresses": [
           |    {
           |      "addressLine1": "prepopLine1",
           |      "addressLine2": "prepopLine2",
           |      "postcode": "wrongPostcode"
           |    },
           |    {
           |      "addressLine1": "prepopLine11",
           |      "addressLine2": "prepopLine22",
           |      "addressLine3": "prepopLine33",
           |      "country": "prepopCountry"
           |    }
           |  ]
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc},
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() shouldBe 0
      an[Exception] shouldBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
    }

    "show the page with prepop addresses if data is returned from Business Registration" in {
      val addresses =
        s"""{
           |  "addresses": [
           |    {
           |      "addressLine1": "prepopLine1",
           |      "addressLine2": "prepopLine2",
           |      "postcode": "AB9 8ZZ"
           |    },
           |    {
           |      "addressLine1": "prepopLine11",
           |      "addressLine2": "prepopLine22",
           |      "addressLine3": "prepopLine33",
           |      "country": "prepopCountry"
           |    }
           |  ]
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/PrePopAddresses", 200, dummyS4LResponse)

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

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "Where will the company carry out most of its business activities?"
      document.getElementById("ro-address-line-1").text shouldBe "11"
      document.getElementById("ppob-address-line-1").text shouldBe "22"

      document.getElementById("chosenAddress-prepopaddress0").attr("value") shouldBe "prepopAddress0"
      document.getElementById("chosenAddress-prepopaddress0").attr("name") shouldBe "chosenAddress"
      document.getElementById("prepopaddress0-address-line-1").text shouldBe "prepopLine1"
      document.getElementById("prepopaddress0-address-line-2").text shouldBe ", prepopLine2"
      document.getElementById("prepopaddress0-post-code").text shouldBe ", AB9 8ZZ"

      document.getElementById("chosenAddress-prepopaddress1").attr("value") shouldBe "prepopAddress1"
      document.getElementById("chosenAddress-prepopaddress1").attr("name") shouldBe "chosenAddress"
      document.getElementById("prepopaddress1-address-line-1").text shouldBe "prepopLine11"
      document.getElementById("prepopaddress1-address-line-2").text shouldBe ", prepopLine22"
      document.getElementById("prepopaddress1-address-line-3").text shouldBe ", prepopLine33"
      document.getElementById("prepopaddress1-country").text shouldBe ", prepopCountry"
    }
  }

  "POST PPOB Address" should {
    "save to microservice with full company details data and send Audit Event" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

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

      val fResponse = buildClient("/where-company-carries-out-business-activities").
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

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      (jsonAudit \ "auditSource").as[JsString].value shouldBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value shouldBe "registeredOfficeUsedAsPrincipalPlaceOfBusiness"
      (jsonAudit \ "detail" \ "externalUserId").as[JsString].value shouldBe "Ext-xxx"
      (jsonAudit \ "detail" \ "authProviderId").as[JsString].value shouldBe "testAuthProviderId"
      (jsonAudit \ "detail" \ "journeyId").as[JsString].value shouldBe regId
      (jsonAudit \ "detail" \ "registeredOfficeAddress").as[JsString].value shouldBe "true"
    }

    "save to save for later with incomplete company details data and send Audit Event" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

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

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "chosenAddress"->Seq("roAddress")
        ))

      val response = await(fResponse)

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      (jsonAudit \ "auditSource").as[JsString].value shouldBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value shouldBe "registeredOfficeUsedAsPrincipalPlaceOfBusiness"
    }

    "save to microservice with full company details data and prepop address and no Audit Event sent" in {
      val csrfToken = UUID.randomUUID().toString
      val addresses =
        s"""{
           |  "13": {
           |    "line1": "prepopLine1",
           |    "line2": "prepopLine2",
           |    "postCode": "prepopPC0"
           |  },
           |  "1": {
           |    "line1": "prepopLine11",
           |    "line2": "prepopLine22",
           |    "line3": "prepopLine33",
           |    "postCode": "prepopPC1"
           |  }
           |}""".stripMargin
      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val ppobDoc = s"""{"line1":"prepopLine1","line2":"prepopLine2","postCode":"prepopPC0"}"""
      val payeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "tName",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${roDoc},
           |"businessContactDetails": {
           |     "email": "email@email.zzz",
           |     "mobileNumber": "1234567890",
           |     "phoneNumber": "0987654321"
           |  }
           |}""".stripMargin
      val updatedPayeDoc =
        s"""{
           |"companyName": "${companyName}",
           |"tradingName": "tName",
           |"roAddress": ${roDoc},
           |"ppobAddress": ${ppobDoc},
           |"businessContactDetails": {
           |     "email": "email@email.zzz",
           |     "mobileNumber": "1234567890",
           |     "phoneNumber": "0987654321"
           |  }
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubS4LGet(regId, "CompanyDetails", payeDoc)
      stubS4LPut(regId, "CompanyDetails", payeDoc)
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)
      stubPatch(s"/paye-registration/${regId}/company-details", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/${regId}/company-details", 200, payeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/${regId}/company-details")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json shouldBe Json.parse(updatedPayeDoc)

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      reqPostsAudit.size shouldBe 0
    }

    "return an error page when fail saving to microservice with full company details data and prepop address" in {
      val csrfToken = UUID.randomUUID().toString
      val addresses =
        s"""{
           |  "1": {
           |    "line1": "prepopLine11",
           |    "line2": "prepopLine22",
           |    "line3": "prepopLine33",
           |    "postCode": "prepopPC1"
           |  }
           |}""".stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/save4later/paye-registration-frontend/${regId}", 200, "")
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status shouldBe 500
    }
  }

  "GET savePPOBAddress" should {
    val addressLookupID = "888"
    val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
    val payeDoc =s"""{
                    |"companyName": "${companyName}",
                    |"tradingName": "tName",
                    |"roAddress": ${roDoc},
                    |"ppobAddress": ${roDoc},
                    |"businessContactDetails": {
                    |     "email": "email@email.zzz",
                    |     "mobileNumber": "1234567890",
                    |     "phoneNumber": "0987654321"
                    |  }
                    |}""".stripMargin

    "upsert Company Details in PAYE Registration and upsert addresses in Business Registration with an address from Address Lookup" in {
      val addressAuditRef = "tstAuditRef"
      val addressLine1 = "14 St Test Walker"
      val addressLine2 = "Testford"
      val addressLine3 = "Testley"
      val addressLine4 = "Testshire"
      val addressPostcode = "TE1 1ST"
      val addressFromALF = s"""{
                              |  "auditRef":"$addressAuditRef",
                              |  "address":{
                              |    "lines":[
                              |      "$addressLine1",
                              |      "$addressLine2",
                              |      "$addressLine3",
                              |      "$addressLine4"
                              |    ],
                              |    "postcode":"$addressPostcode",
                              |    "country":{
                              |      "code":"UK",
                              |      "name":"United Kingdom"
                              |    }
                              |  }
                              |}""".stripMargin

      val newAddress2BusReg =
        s"""
           |{
           |   "auditRef": "$addressAuditRef",
           |   "addressLine1": "$addressLine1",
           |   "addressLine2": "$addressLine2",
           |   "addressLine3": "$addressLine3",
           |   "addressLine4": "$addressLine4",
           |   "postcode": "$addressPostcode"
           |}
       """.stripMargin

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)
      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubPatch(s"/paye-registration/${regId}/company-details", 200, payeDoc)
      stubGet(s"/paye-registration/${regId}/company-details", 200, payeDoc)
      stubS4LPut(regId, "CompanyDetails", payeDoc)
      stubPost(s"/business-registration/${regId}/addresses", 200, newAddress2BusReg)
      stubDelete(s"/save4later/paye-registration-frontend/${regId}", 200, "")
      stubGet(s"/api/confirmed\\?id\\=$addressLookupID", 200, addressFromALF)

      val response = await(buildClient(s"/return-from-address-for-ppob?id=$addressLookupID")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 303
      response.header(HeaderNames.LOCATION) shouldBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/${regId}/addresses")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json shouldBe Json.parse(newAddress2BusReg)
    }
  }

  "GET Business Contact Details" should {

    val contactDetails = {
      s"""
         |{
         |  "firstName": "fName",
         |  "middleName": "mName",
         |  "surname": "sName",
         |  "email": "email@email.zzz",
         |  "telephoneNumber": "0987654321",
         |  "mobileNumber": "1234567890"
         |}""".stripMargin
    }

    "get prepoulated from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, contactDetails)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") shouldNot be("")
      mdtpCookieData("sessionId") shouldBe SessionId
      mdtpCookieData("userId") shouldBe userId

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "What are the company contact details?"
      document.getElementById("businessEmail").attr("value") shouldBe "email@email.zzz"
      document.getElementById("mobileNumber").attr("value") shouldBe "1234567890"
      document.getElementById("phoneNumber").attr("value") shouldBe "0987654321"
    }

    "get prepoulated from Paye Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |   "companyName": "$companyName",
                      |   "roAddress": $roDoc,
                      |   "ppobAddress": $roDoc,
                      |   "businessContactDetails": {
                      |     "email": "email@email.zzz",
                      |     "mobileNumber": "1234567890",
                      |     "phoneNumber": "0987654321"
                      |   }
                      |}""".stripMargin

      stubGet(s"/save4later/paye-registration-frontend/${regId}", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regId}/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "What are the company contact details?"
      document.getElementById("businessEmail").attr("value") shouldBe "email@email.zzz"
      document.getElementById("mobileNumber").attr("value") shouldBe "1234567890"
      document.getElementById("phoneNumber").attr("value") shouldBe "0987654321"
    }

    "not be prepoulated if no data is found in Business Registration or Paye Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/paye-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 404, "")

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc}
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "What are the company contact details?"
      document.getElementById("businessEmail").attr("value") shouldBe ""
      document.getElementById("mobileNumber").attr("value") shouldBe ""
      document.getElementById("phoneNumber").attr("value") shouldBe ""
    }

    "not be prepoulated if no data is found in Paye Registration and error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubKeystoreMetadata(SessionId, regId, companyName)

      stubGet(s"/paye-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 403, "")

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "${companyName}",
                      |"tradingName": {"differentName":false},
                      |"roAddress": ${roDoc}
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status shouldBe 200

      val document = Jsoup.parse(response.body)
      document.title() shouldBe "What are the company contact details?"
      document.getElementById("businessEmail").attr("value") shouldBe ""
      document.getElementById("mobileNumber").attr("value") shouldBe ""
      document.getElementById("phoneNumber").attr("value") shouldBe ""
    }
  }
}
