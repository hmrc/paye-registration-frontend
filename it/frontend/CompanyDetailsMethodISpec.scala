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
package frontend

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import enums.CacheKeys
import itutil.{CachingStub, IntegrationSpecBase, LoginStub, WiremockHelper}
import models.DigitalContactDetails
import org.jsoup.Jsoup
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.libs.json._
import play.api.test.FakeApplication


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
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.business-registration.host" -> s"$mockHost",
    "microservice.services.business-registration.port" -> s"$mockPort",
    "microservice.services.address-lookup-frontend.host" -> s"$mockHost",
    "microservice.services.address-lookup-frontend.port" -> s"$mockPort",
    "regIdWhitelist" -> "cmVnV2hpdGVsaXN0MTIzLHJlZ1doaXRlbGlzdDQ1Ng==",
    "defaultCTStatus" -> "aGVsZA==",
    "defaultCompanyName" -> "VEVTVC1ERUZBVUxULUNPTVBBTlktTkFNRQ==",
    "defaultCHROAddress" -> "eyJsaW5lMSI6IjE0IFRlc3QgRGVmYXVsdCBTdHJlZXQiLCJsaW5lMiI6IlRlc3RsZXkiLCJsaW5lMyI6IlRlc3Rmb3JkIiwibGluZTQiOiJUZXN0c2hpcmUiLCJwb3N0Q29kZSI6IlRFMSAzU1QifQ==",
    "defaultSeqDirector" -> "W3siZGlyZWN0b3IiOnsiZm9yZW5hbWUiOiJmYXVsdHkiLCJzdXJuYW1lIjoiZGVmYXVsdCJ9fV0=",
    "mongodb.uri" -> s"$mongoUri"
  ))

  override def beforeEach() {
    resetWiremock()
  }

  val regId = "3"
  val txId = "12345"
  val companyName = "Foo Ltd"
  val tradingNameFromPrePop = """
      |{
      | "tradingName" : "fooBarWizz From Pre Pop"
      |}
    """.stripMargin


  "GET Trading Name" should {

    "Return a populated page if PayeReg returns an company details response" in {
      val tradingName = "Foo Trading"
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)

      val companyProfileDoc =
        """
          |{
          |  "company_name":"test company",
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

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)

      document.title() mustBe "Does or will the company trade using a different name?"
      document.getElementById("pageHeading").text mustBe "Does or will the company trade using a different name?"
      document.getElementById("differentName-true").attr("checked") mustBe "checked"
      document.getElementById("differentName-false").attr("checked") mustBe ""
      document.getElementById("tradingName").`val` mustBe tradingName
      document.getElementById("lead-paragraph").text mustBe "Tell us if the company will use a trading name that's different from test company."
    }

    "Return a populated from pre pop page if PayeReg returns a NotFound response" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/trading-name", 200, tradingNameFromPrePop)
      val companyProfileDoc =
        """
          |{
          |  "company_name":"test company",
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
      val crDoc = s"""{"transaction-id": "$txId"}"""
      stubGet(s"/incorporation-frontend-stubs/corporation-tax-registration/$regId/confirmation-references", 200, crDoc)
      val roDoc = s"""{"premises":"p", "address_line_1":"1", "locality":"l"}"""
      stubGet(s"/incorporation-frontend-stubs/$txId/ro-address", 200, roDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "Does or will the company trade using a different name?"
      document.getElementById("pageHeading").text mustBe "Does or will the company trade using a different name?"
      document.getElementById("differentName-true").attr("checked") mustBe ""
      document.getElementById("differentName-false").attr("checked") mustBe ""
      document.getElementById("tradingName").`val` mustBe "fooBarWizz From Pre Pop"

    }

    "Return a populated page with a default Company Name if the regId is part of the whitelist with trading name pre populated" in {
      val regIdWhitelisted = "regWhitelist123"

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regIdWhitelisted)
      stubSessionCacheMetadata(SessionId, regIdWhitelisted)


      stubGet(s"/save4later/paye-registration-frontend/$regIdWhitelisted", 404, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regIdWhitelisted/data/CompanyDetails", 200, dummyS4LResponse)
      stubGet(s"/business-registration/$regIdWhitelisted/trading-name", 200, tradingNameFromPrePop)
      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val document = Jsoup.parse(response.body)
      document.title() mustBe "Does or will the company trade using a different name?"
      document.getElementById("pageHeading").text mustBe "Does or will the company trade using a different name?"
      document.getElementById("differentName-true").attr("checked") mustBe ""
      document.getElementById("differentName-false").attr("checked") mustBe "checked"
      document.getElementById("tradingName").`val` mustBe "fooBarWizz From Pre Pop"
      document.getElementById("lead-paragraph").html.contains("TEST-DEFAULT-COMPANY-NAME") mustBe true
    }
  }

  "POST Trading Name" should {

    "Accept information and send to PR" in {
      val tradingName = "Foo Trading"
      val tradingNameJsonResponse =
        """ {
          | "tradingName": "Foo Trading"
          | }
        """.stripMargin
      val csrfToken = UUID.randomUUID().toString

      setupSimpleAuthMocks()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)

      val companyProfileDoc =
        """
          |{
          |  "company_name":"test company",
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
      stubPost(s"/business-registration/$regId/trading-name", 200, tradingNameJsonResponse)
      val roDoc = s"""{"line1":"1", "line2":"2", "postCode":"pc"}"""
      val payeDoc =s"""{
           |"companyName": "$companyName",
           |"tradingName": {"differentName":false},
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc,
           |"businessContactDetails": {}
           |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val updatedPayeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": "$tradingName",
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc,
           |"businessContactDetails": {}
           |}""".stripMargin
      stubPatch(s"/paye-registration/$regId/company-details", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      val fResponse = buildClient("/trading-name").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "differentName"->Seq("true"),
          "tradingName"->Seq(tradingName)
        ))

      val response = await(fResponse)

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/confirm-registered-office-address")

      val crPuts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/$regId/company-details")))
      val prePopPost = findAll(postRequestedFor(urlMatching(s"/business-registration/$regId/trading-name")))
      val captor = crPuts.get(0)
      val json = Json.parse(captor.getBodyAsString)
      (json \ "tradingName").as[String] mustBe tradingName
      val jsonOfPrePopPost =  Json.parse(prePopPost.get(0).getBodyAsString)
      (jsonOfPrePopPost \ "tradingName").as[String] mustBe tradingName
    }
  }

  "GET RO Address page" should {
    "show the page with a default RO Address if the regId is part of the whitelist" in {
      val regIdWhitelisted = "regWhitelist123"
      val defaultCompanyName = "TEST-DEFAULT-COMPANY-NAME"

      setupSimpleAuthMocks()

      stubSuccessfulLogin()

      stubSessionCacheMetadata(SessionId, regIdWhitelisted)

      stubPayeRegDocumentStatus(regIdWhitelisted)

      stubGet(s"/save4later/paye-registration-frontend/${regIdWhitelisted}", 404, "")

      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/${regIdWhitelisted}/data/CompanyDetails", 200, dummyS4LResponse)

      val fResponse = buildClient("/confirm-registered-office-address").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe s"Confirm $defaultCompanyName's registered office address"
      document.getElementById("ro-address-address-line-1").text mustBe "14 Test Default Street"
    }
  }

  "GET PPOB page" should {
    "show the page with the RO Address when there is no PPOB Address" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ro-address-line-1").text mustBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() mustBe 0
    }

    "show the page with the PPOB Address when PPOB Address is the same as RO Address" in {

      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"ppobAddress": $roDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ppob-address-line-1").text mustBe "11"
      document.getElementsByAttributeValue("id", "ro-address-line-1").size() mustBe 0

    }

    "show the page with both PPOB and RO Addresses when there are two different addresses" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 200, """{"addresses":[]}""")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val ppobDoc = s"""{"line1":"22", "line2":"23", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"ppobAddress": $ppobDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ro-address-line-1").text mustBe "11"
      document.getElementById("ppob-address-line-1").text mustBe "22"

    }

    "show the page without prepop addresses if an error is returned from Business Registration" in {
      setupSimpleAuthMocks()
      stubSuccessfulLogin()
      stubPayeRegDocumentStatus(regId)
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 403, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ro-address-line-1").text mustBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() mustBe 0
      an[Exception] mustBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
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
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ro-address-line-1").text mustBe "11"
      document.getElementsByAttributeValue("id", "ppob-address-line-1").size() mustBe 0
      an[Exception] mustBe thrownBy(document.getElementById("chosenAddress-prepopaddress0").attr("value"))
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
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/business-registration/$regId/addresses", 200, addresses)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/PrePopAddresses", 200, dummyS4LResponse)

      val roDoc = s"""{"line1":"11", "line2":"22", "postCode":"pc1 1pc"}"""
      val ppobDoc = s"""{"line1":"22", "line2":"23", "postCode":"pc1 1pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"ppobAddress": $ppobDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin
      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> getSessionCookie()).
        get()

      val response = await(fResponse)

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What is the company's 'principal place of business'?"
      document.getElementById("ro-address-line-1").text mustBe "11"
      document.getElementById("ppob-address-line-1").text mustBe "22"

      document.getElementById("chosenAddress-prepopaddress0").attr("value") mustBe "prepopAddress0"
      document.getElementById("chosenAddress-prepopaddress0").attr("name") mustBe "chosenAddress"
      document.getElementById("prepopaddress0-address-line-1").text mustBe "prepopLine1"
      document.getElementById("prepopaddress0-address-line-2").text mustBe ", prepopLine2"
      document.getElementById("prepopaddress0-post-code").text mustBe ", AB9 8ZZ"

      document.getElementById("chosenAddress-prepopaddress1").attr("value") mustBe "prepopAddress1"
      document.getElementById("chosenAddress-prepopaddress1").attr("name") mustBe "chosenAddress"
      document.getElementById("prepopaddress1-address-line-1").text mustBe "prepopLine11"
      document.getElementById("prepopaddress1-address-line-2").text mustBe ", prepopLine22"
      document.getElementById("prepopaddress1-address-line-3").text mustBe ", prepopLine33"
      document.getElementById("prepopaddress1-country").text mustBe ", prepopCountry"
    }
  }

  "POST PPOB Address" should {
    "save to microservice with full company details data and send Audit Event" in {
      setupAuthMocks()
      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      val csrfToken = UUID.randomUUID().toString

      stubSessionCacheMetadata(SessionId, regId)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc,
                      |"businessContactDetails": {}
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": "tName",
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc,
           |"businessContactDetails": {}
           |}""".stripMargin
      stubPatch(s"/paye-registration/$regId/company-details", 200, updatedPayeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))

      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken"->Seq("xxx-ignored-xxx"),
          "chosenAddress"->Seq("roAddress")
        ))

      val response = await(fResponse)

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/business-contact-details")

      val crPuts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/$regId/company-details")))
      val captor = crPuts.get(0)
      val json = Json.parse(captor.getBodyAsString)
      (json \ "ppobAddress").as[JsObject].toString() mustBe roDoc

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      (jsonAudit \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value mustBe "registeredOfficeUsedAsPrincipalPlaceOfBusiness"
      (jsonAudit \ "detail" \ "externalUserId").as[JsString].value mustBe "Ext-xxx"
      (jsonAudit \ "detail" \ "authProviderId").as[JsString].value mustBe "testAuthProviderId"
      (jsonAudit \ "detail" \ "journeyId").as[JsString].value mustBe regId
      (jsonAudit \ "detail" \ "registeredOfficeAddress").as[JsString].value mustBe "true"

      val tags = (jsonAudit \ "tags").as[JsObject].value
      tags("clientIP") mustBe Json.toJson("-")
      tags("path") mustBe Json.toJson("/register-for-paye/where-company-carries-out-business-activities")
      tags("clientPort") mustBe Json.toJson("-")
      tags.contains("X-Session-ID") mustBe true
      tags.contains("X-Request-ID") mustBe true
      tags.contains("deviceID") mustBe true
      tags("Authorization") mustBe Json.toJson("-")
      tags("transactionName") mustBe Json.toJson("registeredOfficeUsedAsPrincipalPlaceOfBusiness")
    }


    "save to save for later with incomplete company details data and send Audit Event" in {
      setupAuthMocks()
      stubSuccessfulLogin()

      stubPayeRegDocumentStatus(regId)

      val csrfToken = UUID.randomUUID().toString

      stubSessionCacheMetadata(SessionId, regId)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val updatedPayeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": "tName",
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc
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

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      val captorPost = reqPosts.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      (jsonAudit \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value mustBe "registeredOfficeUsedAsPrincipalPlaceOfBusiness"
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
           |"companyName": "$companyName",
           |"tradingName": "tName",
           |"roAddress": $roDoc,
           |"ppobAddress": $roDoc,
           |"businessContactDetails": {
           |     "email": "email@email.zzz",
           |     "mobileNumber": "1234567890",
           |     "phoneNumber": "0987654321"
           |  }
           |}""".stripMargin
      val updatedPayeDoc =
        s"""{
           |"companyName": "$companyName",
           |"tradingName": "tName",
           |"roAddress": $roDoc,
           |"ppobAddress": $ppobDoc,
           |"businessContactDetails": {
           |     "email": "email@email.zzz",
           |     "mobileNumber": "1234567890",
           |     "phoneNumber": "0987654321"
           |  }
           |}""".stripMargin

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubS4LGet(regId, "CompanyDetails", payeDoc)
      stubS4LPut(regId, "CompanyDetails", payeDoc)
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)
      stubPatch(s"/paye-registration/$regId/company-details", 200, updatedPayeDoc)
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)

      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(patchRequestedFor(urlMatching(s"/paye-registration/$regId/company-details")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json mustBe Json.parse(updatedPayeDoc)

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      reqPostsAudit.size mustBe 0
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

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubS4LGet(regId, CacheKeys.PrePopAddresses.toString, addresses)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/where-company-carries-out-business-activities").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "chosenAddress" -> Seq("prepopAddress13")
        ))

      val response = await(fResponse)
      response.status mustBe 500
    }
  }

  "GET savePPOBAddress" should {
    val addressLookupID = "888"
    val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
    val payeDoc =s"""{
                    |"companyName": "$companyName",
                    |"tradingName": "tName",
                    |"roAddress": $roDoc,
                    |"ppobAddress": $roDoc,
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

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubPatch(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      stubS4LPut(regId, "CompanyDetails", payeDoc)
      stubPost(s"/business-registration/$regId/addresses", 200, newAddress2BusReg)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")
      stubGet(s"/api/confirmed\\?id\\=$addressLookupID", 200, addressFromALF)

      val response = await(buildClient(s"/return-from-address-for-ppob?id=$addressLookupID")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/business-contact-details")

      val reqPosts = findAll(postRequestedFor(urlMatching(s"/business-registration/$regId/addresses")))
      val captor = reqPosts.get(0)
      val json = Json.parse(captor.getBodyAsString)

      json mustBe Json.parse(newAddress2BusReg)
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

    "get prepopulated from Business Registration" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val companyProfileDoc =
        s"""
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
      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, contactDetails)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200
      val mdtpCookieData = getCookieData(response.cookie("mdtp").get)
      mdtpCookieData("csrfToken") mustNot be("")
      mdtpCookieData("sessionId") mustBe SessionId
      mdtpCookieData("userId") mustBe userId

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What are the company's contact details?"
      document.getElementById("businessEmail").attr("value") mustBe "email@email.zzz"
      document.getElementById("mobileNumber").attr("value") mustBe "1234567890"
      document.getElementById("phoneNumber").attr("value") mustBe "0987654321"
    }

    "get prepopulated from Paye Registration" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

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

      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 200, payeDoc)
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What are the company's contact details?"
      document.getElementById("businessEmail").attr("value") mustBe "email@email.zzz"
      document.getElementById("mobileNumber").attr("value") mustBe "1234567890"
      document.getElementById("phoneNumber").attr("value") mustBe "0987654321"
    }

    "not be prepopulated if no data is found in Business Registration or Paye Registration" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/paye-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 404, "")

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What are the company's contact details?"
      document.getElementById("businessEmail").attr("value") mustBe ""
      document.getElementById("mobileNumber").attr("value") mustBe ""
      document.getElementById("phoneNumber").attr("value") mustBe ""
    }

    "not be prepopulated if no data is found in Paye Registration and error is returned from Business Registration" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/paye-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 403, "")

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What are the company's contact details?"
      document.getElementById("businessEmail").attr("value") mustBe ""
      document.getElementById("mobileNumber").attr("value") mustBe ""
      document.getElementById("phoneNumber").attr("value") mustBe ""
    }

    "not be prepopulated if no data is found in Paye Registration and corrupted data is returned from Business Registration" in {
      val corruptedContactDetails = {
        s"""
           |{
           |  "firstName": "fName",
           |  "middleName": "mName",
           |  "surname": "sName",
           |  "email": "email@email.zzz",
           |  "telephoneNumber": "098321",
           |  "mobileNumber": "123456789012345678901"
           |}""".stripMargin
      }

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubGet(s"/paye-registration/$regId/contact-details", 404, "")
      stubGet(s"/business-registration/$regId/contact-details", 200, corruptedContactDetails)

      val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
      val payeDoc =s"""{
                      |"companyName": "$companyName",
                      |"tradingName": {"differentName":false},
                      |"roAddress": $roDoc
                      |}""".stripMargin

      stubS4LGet(regId, "CompanyDetails", payeDoc)

      val response = await(buildClient("/business-contact-details")
        .withHeaders(HeaderNames.COOKIE -> getSessionCookie())
        .get())

      response.status mustBe 200

      val document = Jsoup.parse(response.body)
      document.title() mustBe "What are the company's contact details?"
      document.getElementById("businessEmail").attr("value") mustBe ""
      document.getElementById("mobileNumber").attr("value") mustBe ""
      document.getElementById("phoneNumber").attr("value") mustBe ""
    }
  }

  "POST Business Contact Details" should {
    val csrfToken = UUID.randomUUID().toString
    val oldEmail = "oldEmail@email.co.uk"
    val oldTelephoneNumber = "0987654321"
    val oldMobileNumber = "1234567890"
    val newEmail = "newEmail@email.biz.co.uk"
    val newTelephoneNumber = "02123456789"
    val newMobileNumber = "07123456789"

    val roDoc = s"""{"line1":"1","line2":"2","postCode":"pc"}"""
    val payeDoc =s"""{
                    |  "companyName": "$companyName",
                    |  "tradingName": {"differentName":false},
                    |  "roAddress": $roDoc,
                    |  "ppobAddress": $roDoc,
                    |  "businessContactDetails": {
                    |    "email": "$oldEmail",
                    |    "phoneNumber": "$oldTelephoneNumber",
                    |    "mobileNumber": "$oldMobileNumber"
                    |  }
                    |}""".stripMargin

    val updatedPayeDoc =
      s"""{
         |  "companyName": "$companyName",
         |  "roAddress": $roDoc,
         |  "ppobAddress": $roDoc,
         |  "businessContactDetails": {
         |    "email": "$newEmail",
         |    "phoneNumber": "$newTelephoneNumber",
         |    "mobileNumber": "$newMobileNumber"
         |  }
         |}
         """.stripMargin

    "upsert the business contact details in PAYE Registration and send Audit Event if different from Prepopulation" in {
      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      stubS4LGet(regId, CacheKeys.CompanyDetails.toString, payeDoc)
      stubPatch(s"/paye-registration/$regId/company-details", 200, updatedPayeDoc)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/business-contact-details").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "businessEmail" -> Seq(s"$newEmail"),
          "phoneNumber" -> Seq(s"$newTelephoneNumber"),
          "mobileNumber" -> Seq(s"$newMobileNumber")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/what-company-does")

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      reqPostsAudit.size mustBe 1
      val captorPost = reqPostsAudit.get(0)
      val jsonAudit = Json.parse(captorPost.getBodyAsString)

      val previousContactDetails = DigitalContactDetails(
        Some(oldEmail),
        Some(oldMobileNumber),
        Some(oldTelephoneNumber)
      )

      val newContactDetails = DigitalContactDetails(
        Some(newEmail),
        Some(newMobileNumber),
        Some(newTelephoneNumber)
      )

      (jsonAudit \ "auditSource").as[JsString].value mustBe "paye-registration-frontend"
      (jsonAudit \ "auditType").as[JsString].value mustBe "businessContactAmendment"
      (jsonAudit \ "detail" \ "externalUserId").as[JsString].value mustBe "Ext-xxx"
      (jsonAudit \ "detail" \ "authProviderId").as[JsString].value mustBe "testAuthProviderId"
      (jsonAudit \ "detail" \ "journeyId").as[JsString].value mustBe regId
      (jsonAudit \ "detail" \ "previousContactDetails").as[DigitalContactDetails] mustBe previousContactDetails
      (jsonAudit \ "detail" \ "newContactDetails").as[DigitalContactDetails] mustBe newContactDetails

      val tags = (jsonAudit \ "tags").as[JsObject].value
      tags("clientIP") mustBe Json.toJson("-")
      tags("path") mustBe Json.toJson("/register-for-paye/business-contact-details")
      tags("clientPort") mustBe Json.toJson("-")
      tags.contains("X-Session-ID") mustBe true
      tags.contains("X-Request-ID") mustBe true
      tags.contains("deviceID") mustBe true
      tags("Authorization") mustBe Json.toJson("-")
      tags("transactionName") mustBe Json.toJson("businessContactAmendment")
    }

    "upsert the business contact details in PAYE Registration with Prepopulation data and don't send Audit Event" in {
      val contactDetails = {
        s"""
           |{
           |  "firstName": "fName",
           |  "middleName": "mName",
           |  "surname": "sName",
           |  "email": "$newEmail",
           |  "telephoneNumber": "$newTelephoneNumber",
           |  "mobileNumber": "$newMobileNumber"
           |}""".stripMargin
      }

      setupAuthMocks()
      stubSuccessfulLogin()
      stubSessionCacheMetadata(SessionId, regId)

      val companyProfileDoc =
        """
          |{
          |  "company_name":"test company",
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
      stubGet(s"/business-registration/$regId/contact-details", 200, contactDetails)
      stubGet(s"/save4later/paye-registration-frontend/$regId", 404, "")
      stubGet(s"/save4later/paye-registration-frontend/$regId/${CacheKeys.CompanyDetails.toString}", 404, "")
      stubGet(s"/paye-registration/$regId/company-details", 404, "")
      stubPatch(s"/paye-registration/$regId/company-details", 200, updatedPayeDoc)
      stubDelete(s"/save4later/paye-registration-frontend/$regId", 200, "")
      val dummyS4LResponse = s"""{"id":"xxx", "data": {} }"""
      stubPut(s"/save4later/paye-registration-frontend/$regId/data/CompanyDetails", 200, dummyS4LResponse)

      val sessionCookie = getSessionCookie(Map("csrfToken" -> csrfToken))
      val fResponse = buildClient("/business-contact-details").
        withHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").
        post(Map(
          "csrfToken" -> Seq("xxx-ignored-xxx"),
          "businessEmail" -> Seq(s"$newEmail"),
          "phoneNumber" -> Seq(s"$newTelephoneNumber"),
          "mobileNumber" -> Seq(s"$newMobileNumber")
        ))

      val response = await(fResponse)
      response.status mustBe 303
      response.header(HeaderNames.LOCATION) mustBe Some("/register-for-paye/what-company-does")

      val reqPostsAudit = findAll(postRequestedFor(urlMatching(s"/write/audit")))
      reqPostsAudit.size mustBe 0
    }
  }
}
